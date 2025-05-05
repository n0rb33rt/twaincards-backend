package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.LearningHistoryDTO;
import com.norbert.twaincards.dto.UserStatisticsDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.LearningHistory;
import com.norbert.twaincards.entity.StudySession;
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
  private final StudySessionRepository studySessionRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @PersistenceContext
  private EntityManager entityManager;


  @Scheduled(cron = "0 0 0 * * ?")
  public void resetInactiveStreaks() {
    entityManager.createNativeQuery("SELECT reset_inactive_streaks()")
            .executeUpdate();
    log.info("Reset inactive streaks");
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
              .build());

      String dayOfWeek = date.getDayOfWeek().toString();
      activityByWeekday.put(dayOfWeek, activityByWeekday.getOrDefault(dayOfWeek, 0) + (int) cardsStudied);
    }

    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> UserStatistics.builder().user(user).build());

    return UserStatisticsDTO.ActivityStatisticsDTO.builder()
            .totalDaysActive(historyByDay.size())
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

    List<StudySession> studySessions = studySessionRepository.findRecentSessions(user, startDate);

    List<StudySession> completedSessions = studySessions.stream()
        .filter(StudySession::getIsCompleted)
        .toList();

    int studySessionsCount = completedSessions.size();

    Long totalCardsReviewed = studySessionRepository.countCardsReviewedInPeriod(user, startDate);
    totalCardsReviewed = totalCardsReviewed != null ? totalCardsReviewed : 0L;
    
    Long totalCorrectAnswers = studySessionRepository.countCorrectAnswersInPeriod(user, startDate);
    totalCorrectAnswers = totalCorrectAnswers != null ? totalCorrectAnswers : 0L;
    
    Long totalIncorrectAnswers = totalCardsReviewed - totalCorrectAnswers;

    double successRate = totalCardsReviewed > 0 ? (totalCorrectAnswers * 100.0) / totalCardsReviewed : 0;

    List<LearningHistory> reviewHistory = learningHistoryRepository.findByUserAndPerformedAtBetween(user, startDate, endDate)
            .stream()
            .toList();
            
    long uniqueCardsStudied = reviewHistory.stream()
            .map(LearningHistory::getCard)
            .distinct()
            .count();

    return LearningHistoryDTO.SummaryStatisticsDTO.builder()
            .totalReviews(totalCardsReviewed)
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