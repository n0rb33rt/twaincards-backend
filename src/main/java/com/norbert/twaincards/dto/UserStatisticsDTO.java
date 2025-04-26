package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO для передачі даних статистики користувача
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {

  private Long id;
  private Long userId;
  private String username;

  private Integer totalCards;
  private Integer learnedCards;
  private Integer totalStudyTimeMinutes;
  private Integer learningStreakDays;
  private LocalDate lastStudyDate;
  private LocalDateTime updatedAt;

  // Розрахункові поля
  private Double completionPercentage;
  private Integer cardsToReview;
  private Integer newCardsToLearn;

  /**
   * DTO для детальної статистики за мовами
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LanguageStatisticsDTO {
    private Long languageId;
    private String languageCode;
    private String languageName;
    private Integer totalCards;
    private Integer learnedCards;
    private Double completionPercentage;
  }

  /**
   * DTO для зведеної статистики активності
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivityStatisticsDTO {
    private Integer totalDaysActive;
    private Integer maxStreakDays;
    private Integer currentStreakDays;
    private Integer averageCardsPerDay;
    private Map<String, Integer> activityByWeekday;
    private List<DailyActivityDTO> dailyActivity;
  }

  /**
   * DTO для статистики активності за день
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyActivityDTO {
    private LocalDate date;
    private Integer cardsStudied;
    private Integer newCardsLearned;
    private Integer minutesSpent;
  }

  /**
   * DTO для глобальної статистики системи
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GlobalStatisticsDTO {
    private Long totalUsers;
    private Long totalCards;
    private Long totalCollections;
    private Long totalLearnedCards;
    private Long totalStudyTimeMinutes;
    private Double averageCompletionPercentage;
  }
}