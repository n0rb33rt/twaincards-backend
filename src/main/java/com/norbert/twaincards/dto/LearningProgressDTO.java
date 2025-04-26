package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для передачі даних прогресу вивчення
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressDTO {

  private Long id;
  private Long userId;
  private Long cardId;

  // Інформація про картку
  private String frontText;
  private String backText;
  private Long collectionId;
  private String collectionName;

  private Integer repetitionCount;
  private Integer correctAnswers;
  private Integer incorrectAnswers;
  private BigDecimal easeFactor;
  private LocalDateTime nextReviewDate;
  private String learningStatus;
  private LocalDateTime lastReviewedAt;

  // Розрахункові поля
  private Double successRate;

  /**
   * DTO для статистики вивчення за статусами
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatusStatisticsDTO {
    private String status;
    private Long count;
  }

  /**
   * DTO для статистики вивчення за часом
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeStatisticsDTO {
    private LocalDateTime date;
    private Long newCards;
    private Long learningCards;
    private Long reviewCards;
    private Long knownCards;
  }
}