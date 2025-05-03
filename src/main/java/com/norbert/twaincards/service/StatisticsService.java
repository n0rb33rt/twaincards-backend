package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.LearningHistoryDTO;
import com.norbert.twaincards.dto.UserStatisticsDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.LearningHistory;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserStatistics;
import com.norbert.twaincards.entity.enumeration.ActionType;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.*;
import com.norbert.twaincards.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

  private final UserRepository userRepository;
  private final UserStatisticsRepository userStatisticsRepository;
  private final LearningProgressRepository learningProgressRepository;
  private final LearningHistoryRepository learningHistoryRepository;
  private final CollectionRepository collectionRepository;
  private final CardRepository cardRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @PersistenceContext
  private EntityManager entityManager;

  @Scheduled(cron = "0 0 3 * * ?")
  public void updateDailyStatistics() {
    log.info("Starting daily statistics update");
    try {
      updateAllUserStatistics();
      log.info("Daily statistics update completed successfully");
    } catch (Exception e) {
      log.error("Error during daily statistics update", e);
    }
  }

  @Transactional
  public UserStatisticsDTO getUserStatistics() {
    User user = securityUtils.getCurrentUser();
    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> {
              UserStatistics newStats = UserStatistics.builder().user(user).build();
              return userStatisticsRepository.save(newStats);
            });

    UserStatisticsDTO statisticsDTO = convertToDto(statistics);

    LocalDateTime now = LocalDateTime.now();
    statisticsDTO.setCardsToReview(learningProgressRepository.findCardsForReview(user, now).size());

    return statisticsDTO;
  }

  @Transactional(readOnly = true)
  public List<UserStatisticsDTO.LanguageStatisticsDTO> getLanguageStatistics() {
    User user = securityUtils.getCurrentUser();
    List<Collection> collections = collectionRepository.findByUser(user);

    Map<Long, List<Collection>> collectionsByLanguage = collections.stream()
            .collect(Collectors.groupingBy(
                    collection -> collection.getTargetLanguage().getId()
            ));

    List<UserStatisticsDTO.LanguageStatisticsDTO> languageStats = new ArrayList<>();

    for (Map.Entry<Long, List<Collection>> entry : collectionsByLanguage.entrySet()) {
      Long languageId = entry.getKey();
      List<Collection> languageCollections = entry.getValue();

      int totalCards = 0;
      for (Collection collection : languageCollections) {
        totalCards += cardRepository.countByCollectionId(collection.getId()).intValue();
      }

      List<Long> collectionIds = languageCollections.stream()
              .map(Collection::getId)
              .toList();

      int learnedCards = 0;
      for (Long collectionId : collectionIds) {
        learnedCards += (int) learningProgressRepository.getStatusStatisticsByUserAndCollection(user, collectionId).stream()
                .filter(row -> "KNOWN".equals(row[0].toString()))
                .mapToLong(row -> (Long) row[1])
                .sum();
      }

      double completionPercentage = totalCards > 0 ? (learnedCards * 100.0) / totalCards : 0;

      UserStatisticsDTO.LanguageStatisticsDTO languageStatsDTO = UserStatisticsDTO.LanguageStatisticsDTO.builder()
              .languageId(languageId)
              .languageCode(languageCollections.get(0).getTargetLanguage().getCode())
              .languageName(languageCollections.get(0).getTargetLanguage().getName())
              .totalCards(totalCards)
              .learnedCards(learnedCards)
              .completionPercentage(Math.round(completionPercentage * 100) / 100.0)
              .build();

      languageStats.add(languageStatsDTO);
    }

    return languageStats;
  }

  @Transactional(readOnly = true)
  public UserStatisticsDTO.ActivityStatisticsDTO getActivityStatistics(int days) {
    User user = securityUtils.getCurrentUser();
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);

    List<LearningHistory> history = learningHistoryRepository.findByUserAndPerformedAtBetween(user, startDate, endDate);

    Map<LocalDate, List<LearningHistory>> historyByDay = history.stream()
            .collect(Collectors.groupingBy(
                    h -> h.getPerformedAt().toLocalDate()
            ));

    List<UserStatisticsDTO.DailyActivityDTO> dailyActivity = new ArrayList<>();
    Map<String, Integer> activityByWeekday = new HashMap<>();

    for (int i = 0; i < days; i++) {
      LocalDate date = endDate.toLocalDate().minusDays(i);
      List<LearningHistory> dayHistory = historyByDay.getOrDefault(date, Collections.emptyList());

      long cardsStudied = dayHistory.stream()
              .filter(h -> h.getActionType() == ActionType.REVIEW)
              .map(LearningHistory::getCard)
              .distinct()
              .count();

      long newCardsLearned = dayHistory.stream()
              .filter(h -> h.getActionType() == ActionType.REVIEW)
              .filter(h -> h.getIsCorrect() != null && h.getIsCorrect())
              .map(LearningHistory::getCard)
              .distinct()
              .count();


      dailyActivity.add(UserStatisticsDTO.DailyActivityDTO.builder()
              .date(date)
              .cardsStudied((int) cardsStudied)
              .newCardsLearned((int) newCardsLearned)
              .build());

      String dayOfWeek = date.getDayOfWeek().toString();
      activityByWeekday.put(dayOfWeek, activityByWeekday.getOrDefault(dayOfWeek, 0) + (int) cardsStudied);
    }

    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> UserStatistics.builder().user(user).build());

    return UserStatisticsDTO.ActivityStatisticsDTO.builder()
            .totalDaysActive(historyByDay.size())
            .maxStreakDays(calculateMaxStreak(historyByDay.keySet()))
            .currentStreakDays(statistics.getLearningStreakDays())
            .averageCardsPerDay(calculateAverageCardsPerDay(dailyActivity))
            .activityByWeekday(activityByWeekday)
            .dailyActivity(dailyActivity)
            .build();
  }

  @Transactional(readOnly = true)
  public UserStatisticsDTO getCollectionStatistics(Long collectionId) {
    User user = securityUtils.getCurrentUser();
    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> UserStatistics.builder().user(user).build());

    UserStatisticsDTO statisticsDTO = convertToDto(statistics);

    long totalCards = cardRepository.countByCollectionId(collectionId);
    statisticsDTO.setTotalCards((int) totalCards);

    long learnedCards = learningProgressRepository.getStatusStatisticsByUserAndCollection(user, collectionId).stream()
            .filter(row -> "KNOWN".equals(row[0].toString()))
            .mapToLong(row -> (Long) row[1])
            .sum();
    statisticsDTO.setLearnedCards((int) learnedCards);

    double completionPercentage = totalCards > 0 ? (learnedCards * 100.0) / totalCards : 0;
    statisticsDTO.setCompletionPercentage(Math.round(completionPercentage * 100) / 100.0);

    LocalDateTime now = LocalDateTime.now();
    int cardsToReview = learningProgressRepository.findCardsForReviewByCollection(user, collectionId, now).size();
    statisticsDTO.setCardsToReview(cardsToReview);

    return statisticsDTO;
  }

  @Transactional(readOnly = true)
  public LearningHistoryDTO.SummaryStatisticsDTO getSummaryStatistics(int days) {
    User user = securityUtils.getCurrentUser();
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);

    List<LearningHistory> history = learningHistoryRepository.findByUserAndPerformedAtBetween(user, startDate, endDate);

    List<LearningHistory> reviewHistory = history.stream()
            .filter(h -> h.getActionType() == ActionType.REVIEW)
            .toList();

    long totalReviews = reviewHistory.size();

    long totalCorrectAnswers = reviewHistory.stream()
            .filter(h -> h.getIsCorrect() != null && h.getIsCorrect())
            .count();

    long totalIncorrectAnswers = reviewHistory.stream()
            .filter(h -> h.getIsCorrect() != null && !h.getIsCorrect())
            .count();

    double successRate = totalReviews > 0 ? (totalCorrectAnswers * 100.0) / totalReviews : 0;


    long uniqueCardsStudied = reviewHistory.stream()
            .map(LearningHistory::getCard)
            .distinct()
            .count();

    Map<LocalDate, List<LearningHistory>> historyByDay = reviewHistory.stream()
            .collect(Collectors.groupingBy(
                    h -> h.getPerformedAt().toLocalDate()
            ));

    int studySessionsCount = historyByDay.size();


    return LearningHistoryDTO.SummaryStatisticsDTO.builder()
            .totalReviews(totalReviews)
            .totalCorrectAnswers(totalCorrectAnswers)
            .totalIncorrectAnswers(totalIncorrectAnswers)
            .successRate(Math.round(successRate * 100) / 100.0)
            .uniqueCardsStudied(uniqueCardsStudied)
            .studySessionsCount(studySessionsCount)
            .build();
  }

  @Transactional(readOnly = true)
  public List<UserStatisticsDTO> getTopUsersByLearnedCards(int limit) {
    List<UserStatistics> topUsers = userStatisticsRepository.findTopUsersByLearnedCards(PageRequest.of(0, limit));

    return topUsers.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<UserStatisticsDTO> getTopUsersByLearningStreak(int limit) {
    List<UserStatistics> topUsers = userStatisticsRepository.findTopUsersByLearningStreak(PageRequest.of(0, limit));

    return topUsers.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public UserStatisticsDTO.GlobalStatisticsDTO getGlobalStatistics() {
    long totalUsers = userRepository.count();
    long totalCollections = collectionRepository.count();
    long totalCards = cardRepository.count();

    Object[] globalStats = userStatisticsRepository.getGlobalStatistics();

    long totalLearnedCards = 0;
    long totalStudyTimeMinutes = 0;

    if (globalStats != null && globalStats.length >= 3) {
      totalCards = globalStats[0] != null ? ((Number) globalStats[0]).longValue() : 0;
      totalLearnedCards = globalStats[1] != null ? ((Number) globalStats[1]).longValue() : 0;
      totalStudyTimeMinutes = globalStats[2] != null ? ((Number) globalStats[2]).longValue() : 0;
    }

    Double averageCompletionPercentage = userStatisticsRepository.getAverageCompletionPercentage();

    return UserStatisticsDTO.GlobalStatisticsDTO.builder()
            .totalUsers(totalUsers)
            .totalCards(totalCards)
            .totalCollections(totalCollections)
            .totalLearnedCards(totalLearnedCards)
            .totalStudyTimeMinutes(totalStudyTimeMinutes)
            .averageCompletionPercentage(averageCompletionPercentage != null ?
                    Math.round(averageCompletionPercentage * 100) / 100.0 : 0)
            .build();
  }

  @Transactional
  public void updateAllUserStatistics() {
    try {
      entityManager.createNativeQuery("CALL update_all_user_statistics()")
              .executeUpdate();
      log.info("Statistics updated for all users");
    } catch (Exception e) {
      log.error("Error updating statistics for all users", e);
    }
  }

  private int calculateMaxStreak(Set<LocalDate> datesSet) {
    if (datesSet.isEmpty()) {
      return 0;
    }

    List<LocalDate> dates = new ArrayList<>(datesSet);
    Collections.sort(dates);

    int maxStreak = 1;
    int currentStreak = 1;

    for (int i = 1; i < dates.size(); i++) {
      LocalDate prevDate = dates.get(i - 1);
      LocalDate currDate = dates.get(i);

      if (ChronoUnit.DAYS.between(prevDate, currDate) == 1) {
        currentStreak++;
        maxStreak = Math.max(maxStreak, currentStreak);
      } else if (ChronoUnit.DAYS.between(prevDate, currDate) > 1) {
        currentStreak = 1;
      }
    }

    return maxStreak;
  }

  private int calculateAverageCardsPerDay(List<UserStatisticsDTO.DailyActivityDTO> dailyActivity) {
    if (dailyActivity.isEmpty()) {
      return 0;
    }

    int totalCards = dailyActivity.stream()
            .mapToInt(UserStatisticsDTO.DailyActivityDTO::getCardsStudied)
            .sum();

    long activeDays = dailyActivity.stream()
            .filter(day -> day.getCardsStudied() > 0)
            .count();

    return activeDays > 0 ? (int) Math.round(totalCards / (double) activeDays) : 0;
  }

  private UserStatisticsDTO convertToDto(UserStatistics statistics) {
    UserStatisticsDTO statisticsDTO = modelMapper.map(statistics, UserStatisticsDTO.class);

    if (statistics.getUser() != null) {
      statisticsDTO.setUserId(statistics.getUser().getId());
      statisticsDTO.setUsername(statistics.getUser().getUsername());
    }

    return statisticsDTO;
  }
}