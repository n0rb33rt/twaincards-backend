package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
  private Integer cardsInProgress;
  private Integer cardsToLearn;
  private Integer learningStreakDays;
  private LocalDate lastStudyDate;
  private LocalDateTime updatedAt;

  private Double completionPercentage;
  private Integer cardsToReview;
  private Integer newCardsToLearn;

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

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyActivityDTO {
    private LocalDate date;
    private Integer cardsStudied;
  }
}