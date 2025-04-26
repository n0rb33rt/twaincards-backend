package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для передачі даних історії вивчення
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryDTO {

  private Long id;
  private Long userId;
  private String username;

  private Long cardId;
  private String frontText;
  private String backText;

  private Long collectionId;
  private String collectionName;

  private String actionType;
  private Boolean isCorrect;
  private Integer responseTimeMs;
  private LocalDateTime performedAt;

  /**
   * DTO для статистики вивчення за днями
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyStatisticsDTO {
    private LocalDateTime date;
    private Long totalReviews;
    private Long correctAnswers;
    private Long incorrectAnswers;
    private Double successRate;
    private Double averageResponseTimeMs;
  }

  /**
   * DTO для зведеної статистики за період
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SummaryStatisticsDTO {
    private Long totalReviews;
    private Long totalCorrectAnswers;
    private Long totalIncorrectAnswers;
    private Double successRate;
    private Double averageResponseTimeMs;
    private Long uniqueCardsStudied;
    private Integer studySessionsCount;
    private Integer totalStudyTimeMinutes;
  }
}