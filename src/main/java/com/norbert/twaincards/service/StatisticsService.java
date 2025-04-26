package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.LearningHistoryDTO;
import com.norbert.twaincards.dto.UserStatisticsDTO;
import com.norbert.twaincards.entity.Collection;
import com.norbert.twaincards.entity.LearningHistory;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.UserStatistics;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи зі статистикою
 */
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

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Отримати статистику користувача
   * @param userId ідентифікатор користувача
   * @return DTO статистики користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */

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


  @Transactional(readOnly = true)
  public UserStatisticsDTO getUserStatistics(Long userId) {
    log.debug("Getting statistics for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> {
              UserStatistics newStats = UserStatistics.builder().user(user).build();
              return userStatisticsRepository.save(newStats);
            });

    UserStatisticsDTO statisticsDTO = convertToDto(statistics);

    // Додаємо додаткову інформацію
    LocalDateTime now = LocalDateTime.now();
    statisticsDTO.setCardsToReview(learningProgressRepository.findCardsForReview(user, now).size());

    return statisticsDTO;
  }

  /**
   * Отримати статистику за мовами для користувача
   * @param userId ідентифікатор користувача
   * @return список DTO статистики за мовами
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public List<UserStatisticsDTO.LanguageStatisticsDTO> getLanguageStatistics(Long userId) {
    log.debug("Getting language statistics for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Отримуємо всі колекції користувача
    List<Collection> collections = collectionRepository.findByUser(user);

    // Групуємо колекції за мовою вивчення (target language)
    Map<Long, List<Collection>> collectionsByLanguage = collections.stream()
            .collect(Collectors.groupingBy(
                    collection -> collection.getTargetLanguage().getId()
            ));

    // Створюємо статистику для кожної мови
    List<UserStatisticsDTO.LanguageStatisticsDTO> languageStats = new ArrayList<>();

    for (Map.Entry<Long, List<Collection>> entry : collectionsByLanguage.entrySet()) {
      Long languageId = entry.getKey();
      List<Collection> languageCollections = entry.getValue();

      // Обчислюємо загальну кількість карток для мови
      int totalCards = 0;
      for (Collection collection : languageCollections) {
        totalCards += cardRepository.countByCollectionId(collection.getId()).intValue();
      }

      // Обчислюємо кількість вивчених карток для мови
      List<Long> collectionIds = languageCollections.stream()
              .map(Collection::getId)
              .collect(Collectors.toList());

      int learnedCards = 0;
      for (Long collectionId : collectionIds) {
        learnedCards += learningProgressRepository.getStatusStatisticsByUserAndCollection(user, collectionId).stream()
                .filter(row -> "KNOWN".equals(row[0].toString()))
                .mapToLong(row -> (Long) row[1])
                .sum();
      }

      // Обчислюємо відсоток вивчення
      double completionPercentage = totalCards > 0 ? (learnedCards * 100.0) / totalCards : 0;

      // Створюємо DTO статистики для мови
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

  /**
   * Отримати статистику активності користувача
   * @param userId ідентифікатор користувача
   * @param days кількість днів для статистики
   * @return DTO статистики активності
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public UserStatisticsDTO.ActivityStatisticsDTO getActivityStatistics(Long userId, int days) {
    log.debug("Getting activity statistics for user with id: {} for the last {} days", userId, days);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);

    // Отримуємо історію вивчення за період
    List<LearningHistory> history = learningHistoryRepository.findByUserAndPerformedAtBetween(user, startDate, endDate);

    // Групуємо історію за днями
    Map<LocalDate, List<LearningHistory>> historyByDay = history.stream()
            .collect(Collectors.groupingBy(
                    h -> h.getPerformedAt().toLocalDate()
            ));

    // Обчислюємо активність за день
    List<UserStatisticsDTO.DailyActivityDTO> dailyActivity = new ArrayList<>();
    Map<String, Integer> activityByWeekday = new HashMap<>();

    for (int i = 0; i < days; i++) {
      LocalDate date = endDate.toLocalDate().minusDays(i);
      List<LearningHistory> dayHistory = historyByDay.getOrDefault(date, Collections.emptyList());

      // Обчислюємо кількість карток, які вивчались у цей день
      long cardsStudied = dayHistory.stream()
              .filter(h -> h.getActionType() == LearningHistory.ActionType.REVIEW)
              .map(LearningHistory::getCard)
              .distinct()
              .count();

      // Обчислюємо кількість нових карток, які почали вивчати у цей день
      long newCardsLearned = dayHistory.stream()
              .filter(h -> h.getActionType() == LearningHistory.ActionType.REVIEW)
              .filter(h -> h.getIsCorrect() != null && h.getIsCorrect())
              .map(LearningHistory::getCard)
              .distinct()
              .count();

      // Обчислюємо час, витрачений на вивчення (у хвилинах)
      int minutesSpent = dayHistory.stream()
              .filter(h -> h.getResponseTimeMs() != null)
              .mapToInt(h -> (int) Math.ceil(h.getResponseTimeMs() / 60000.0))
              .sum();

      // Додаємо активність за день
      dailyActivity.add(UserStatisticsDTO.DailyActivityDTO.builder()
              .date(date)
              .cardsStudied((int) cardsStudied)
              .newCardsLearned((int) newCardsLearned)
              .minutesSpent(minutesSpent)
              .build());

      // Додаємо активність за днем тижня
      String dayOfWeek = date.getDayOfWeek().toString();
      activityByWeekday.put(dayOfWeek, activityByWeekday.getOrDefault(dayOfWeek, 0) + (int) cardsStudied);
    }

    // Отримуємо статистику користувача
    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> UserStatistics.builder().user(user).build());

    // Створюємо DTO статистики активності
    UserStatisticsDTO.ActivityStatisticsDTO activityStats = UserStatisticsDTO.ActivityStatisticsDTO.builder()
            .totalDaysActive((int) historyByDay.keySet().size())
            .maxStreakDays(calculateMaxStreak(historyByDay.keySet()))
            .currentStreakDays(statistics.getLearningStreakDays())
            .averageCardsPerDay(calculateAverageCardsPerDay(dailyActivity))
            .activityByWeekday(activityByWeekday)
            .dailyActivity(dailyActivity)
            .build();

    return activityStats;
  }

  /**
   * Отримати статистику прогресу для колекції
   * @param userId ідентифікатор користувача
   * @param collectionId ідентифікатор колекції
   * @return DTO статистики користувача з інформацією про колекцію
   * @throws ResourceNotFoundException якщо користувача або колекцію не знайдено
   */
  @Transactional(readOnly = true)
  public UserStatisticsDTO getCollectionStatistics(Long userId, Long collectionId) {
    log.debug("Getting collection statistics for user with id: {} and collection with id: {}", userId, collectionId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

    // Отримуємо статистику користувача
    UserStatistics statistics = userStatisticsRepository.findByUser(user)
            .orElseGet(() -> UserStatistics.builder().user(user).build());

    UserStatisticsDTO statisticsDTO = convertToDto(statistics);

    // Обчислюємо загальну кількість карток у колекції
    long totalCards = cardRepository.countByCollectionId(collectionId);
    statisticsDTO.setTotalCards((int) totalCards);

    // Обчислюємо кількість вивчених карток у колекції
    long learnedCards = learningProgressRepository.getStatusStatisticsByUserAndCollection(user, collectionId).stream()
            .filter(row -> "KNOWN".equals(row[0].toString()))
            .mapToLong(row -> (Long) row[1])
            .sum();
    statisticsDTO.setLearnedCards((int) learnedCards);

    // Обчислюємо відсоток вивчення
    double completionPercentage = totalCards > 0 ? (learnedCards * 100.0) / totalCards : 0;
    statisticsDTO.setCompletionPercentage(Math.round(completionPercentage * 100) / 100.0);

    // Обчислюємо кількість карток для повторення
    LocalDateTime now = LocalDateTime.now();
    int cardsToReview = learningProgressRepository.findCardsForReviewByCollection(user, collectionId, now).size();
    statisticsDTO.setCardsToReview(cardsToReview);

    return statisticsDTO;
  }

  /**
   * Отримати статистику за останній період
   * @param userId ідентифікатор користувача
   * @param days кількість днів для статистики
   * @return DTO зведеної статистики
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public LearningHistoryDTO.SummaryStatisticsDTO getSummaryStatistics(Long userId, int days) {
    log.debug("Getting summary statistics for user with id: {} for the last {} days", userId, days);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);

    // Отримуємо історію вивчення за період
    List<LearningHistory> history = learningHistoryRepository.findByUserAndPerformedAtBetween(user, startDate, endDate);

    // Фільтруємо історію за типом дії REVIEW
    List<LearningHistory> reviewHistory = history.stream()
            .filter(h -> h.getActionType() == LearningHistory.ActionType.REVIEW)
            .collect(Collectors.toList());

    // Обчислюємо загальну кількість повторень
    long totalReviews = reviewHistory.size();

    // Обчислюємо кількість правильних та неправильних відповідей
    long totalCorrectAnswers = reviewHistory.stream()
            .filter(h -> h.getIsCorrect() != null && h.getIsCorrect())
            .count();

    long totalIncorrectAnswers = reviewHistory.stream()
            .filter(h -> h.getIsCorrect() != null && !h.getIsCorrect())
            .count();

    // Обчислюємо відсоток успішності
    double successRate = totalReviews > 0 ? (totalCorrectAnswers * 100.0) / totalReviews : 0;

    // Обчислюємо середній час відповіді
    double averageResponseTime = reviewHistory.stream()
            .filter(h -> h.getResponseTimeMs() != null)
            .mapToInt(LearningHistory::getResponseTimeMs)
            .average()
            .orElse(0);

    // Обчислюємо кількість унікальних карток, які вивчались
    long uniqueCardsStudied = reviewHistory.stream()
            .map(LearningHistory::getCard)
            .distinct()
            .count();

    // Групуємо історію за днями для обчислення кількості сесій
    Map<LocalDate, List<LearningHistory>> historyByDay = reviewHistory.stream()
            .collect(Collectors.groupingBy(
                    h -> h.getPerformedAt().toLocalDate()
            ));

    // Обчислюємо кількість сесій навчання (днів, у які були повторення)
    int studySessionsCount = historyByDay.size();

    // Обчислюємо загальний час вивчення (у хвилинах)
    int totalStudyTimeMinutes = reviewHistory.stream()
            .filter(h -> h.getResponseTimeMs() != null)
            .mapToInt(h -> (int) Math.ceil(h.getResponseTimeMs() / 60000.0))
            .sum();

    // Створюємо DTO зведеної статистики
    return LearningHistoryDTO.SummaryStatisticsDTO.builder()
            .totalReviews(totalReviews)
            .totalCorrectAnswers(totalCorrectAnswers)
            .totalIncorrectAnswers(totalIncorrectAnswers)
            .successRate(Math.round(successRate * 100) / 100.0)
            .averageResponseTimeMs(Math.round(averageResponseTime * 100) / 100.0)
            .uniqueCardsStudied(uniqueCardsStudied)
            .studySessionsCount(studySessionsCount)
            .totalStudyTimeMinutes(totalStudyTimeMinutes)
            .build();
  }

  /**
   * Отримати статистику найкращих користувачів
   * @param limit кількість користувачів
   * @return список DTO статистики користувачів
   */
  @Transactional(readOnly = true)
  public List<UserStatisticsDTO> getTopUsersByLearnedCards(int limit) {
    log.debug("Getting top {} users by learned cards", limit);
    List<UserStatistics> topUsers = userStatisticsRepository.findTopUsersByLearnedCards(PageRequest.of(0, limit));

    return topUsers.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати статистику найкращих користувачів за безперервним навчанням
   * @param limit кількість користувачів
   * @return список DTO статистики користувачів
   */
  @Transactional(readOnly = true)
  public List<UserStatisticsDTO> getTopUsersByLearningStreak(int limit) {
    log.debug("Getting top {} users by learning streak", limit);
    List<UserStatistics> topUsers = userStatisticsRepository.findTopUsersByLearningStreak(PageRequest.of(0, limit));

    return topUsers.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати глобальну статистику системи
   * @return DTO глобальної статистики
   */
  @Transactional(readOnly = true)
  public UserStatisticsDTO.GlobalStatisticsDTO getGlobalStatistics() {
    log.debug("Getting global statistics");

    // Обчислюємо загальну кількість користувачів
    long totalUsers = userRepository.count();

    // Обчислюємо загальну кількість колекцій
    long totalCollections = collectionRepository.count();

    // Обчислюємо загальну кількість карток
    long totalCards = cardRepository.count();

    // Отримуємо зведену статистику
    Object[] globalStats = userStatisticsRepository.getGlobalStatistics();

    long totalLearnedCards = 0;
    long totalStudyTimeMinutes = 0;

    if (globalStats != null && globalStats.length >= 3) {
      totalCards = globalStats[0] != null ? ((Number) globalStats[0]).longValue() : 0;
      totalLearnedCards = globalStats[1] != null ? ((Number) globalStats[1]).longValue() : 0;
      totalStudyTimeMinutes = globalStats[2] != null ? ((Number) globalStats[2]).longValue() : 0;
    }

    // Отримуємо середній відсоток виконання
    Double averageCompletionPercentage = userStatisticsRepository.getAverageCompletionPercentage();

    // Створюємо DTO глобальної статистики
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

  /**
   * Оновити статистику всіх користувачів
   * Викликається періодично для оновлення статистики
   */
  @Transactional
  public void updateAllUserStatistics() {
    log.debug("Updating statistics for all users");
    try {
      // Use the directly injected EntityManager instead
      entityManager.createNativeQuery("CALL update_all_user_statistics()")
              .executeUpdate();
      log.info("Statistics updated for all users");
    } catch (Exception e) {
      log.error("Error updating statistics for all users", e);
    }
  }

  /**
   * Обчислити максимальну серію безперервного навчання
   * @param datesSet набір дат, у які були повторення
   * @return кількість днів найдовшої серії
   */
  private int calculateMaxStreak(Set<LocalDate> datesSet) {
    if (datesSet.isEmpty()) {
      return 0;
    }

    // Перетворюємо набір у відсортований список
    List<LocalDate> dates = new ArrayList<>(datesSet);
    Collections.sort(dates);

    int maxStreak = 1;
    int currentStreak = 1;

    for (int i = 1; i < dates.size(); i++) {
      LocalDate prevDate = dates.get(i - 1);
      LocalDate currDate = dates.get(i);

      // Якщо різниця між датами 1 день, серія продовжується
      if (ChronoUnit.DAYS.between(prevDate, currDate) == 1) {
        currentStreak++;
        maxStreak = Math.max(maxStreak, currentStreak);
      } else if (ChronoUnit.DAYS.between(prevDate, currDate) > 1) {
        // Якщо різниця більше 1 дня, серія обривається
        currentStreak = 1;
      }
    }

    return maxStreak;
  }

  /**
   * Обчислити середню кількість карток за день
   * @param dailyActivity список DTO активності за день
   * @return середня кількість карток за день
   */
  private int calculateAverageCardsPerDay(List<UserStatisticsDTO.DailyActivityDTO> dailyActivity) {
    if (dailyActivity.isEmpty()) {
      return 0;
    }

    // Обчислюємо загальну кількість карток
    int totalCards = dailyActivity.stream()
            .mapToInt(UserStatisticsDTO.DailyActivityDTO::getCardsStudied)
            .sum();

    // Обчислюємо кількість днів з активністю
    long activeDays = dailyActivity.stream()
            .filter(day -> day.getCardsStudied() > 0)
            .count();

    // Обчислюємо середню кількість карток за день
    return activeDays > 0 ? (int) Math.round(totalCards / (double) activeDays) : 0;
  }

  /**
   * Конвертувати сутність статистики користувача в DTO
   * @param statistics сутність статистики користувача
   * @return DTO статистики користувача
   */
  private UserStatisticsDTO convertToDto(UserStatistics statistics) {
    UserStatisticsDTO statisticsDTO = modelMapper.map(statistics, UserStatisticsDTO.class);

    // Додаткова інформація про користувача
    if (statistics.getUser() != null) {
      statisticsDTO.setUserId(statistics.getUser().getId());
      statisticsDTO.setUsername(statistics.getUser().getUsername());
    }

    return statisticsDTO;
  }
}