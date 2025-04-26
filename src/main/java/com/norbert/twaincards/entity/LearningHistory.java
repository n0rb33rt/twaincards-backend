package com.norbert.twaincards.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сутність, що представляє історію взаємодії користувача з картками
 */
@Entity
@Table(name = "learning_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "card_id", nullable = false)
  private Card card;

  @Column(name = "action_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private ActionType actionType;

  @Column(name = "is_correct")
  private Boolean isCorrect;

  @Column(name = "response_time_ms")
  private Integer responseTimeMs;

  @CreationTimestamp
  @Column(name = "performed_at", updatable = false)
  private LocalDateTime performedAt;

  /**
   * Типи дій над картками
   */
  public enum ActionType {
    /**
     * Створення картки
     */
    CREATE,

    /**
     * Перегляд картки
     */
    VIEW,

    /**
     * Повторення картки
     */
    REVIEW,

    /**
     * Редагування картки
     */
    EDIT,

    /**
     * Видалення картки
     */
    DELETE
  }
}