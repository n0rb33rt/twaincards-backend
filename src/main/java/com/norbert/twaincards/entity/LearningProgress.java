package com.norbert.twaincards.entity;

import com.norbert.twaincards.entity.enumeration.LearningStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "card_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "card_id", nullable = false)
  private Card card;

  @Column(name = "repetition_count")
  @Builder.Default
  private Integer repetitionCount = 0;

  @Column(name = "correct_answers")
  @Builder.Default
  private Integer correctAnswers = 0;

  @Column(name = "incorrect_answers")
  @Builder.Default
  private Integer incorrectAnswers = 0;

  @Column(name = "next_review_date")
  private LocalDateTime nextReviewDate;

  @Column(name = "learning_status", length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private LearningStatus learningStatus = LearningStatus.NEW;

  @Column(name = "last_reviewed_at")
  private LocalDateTime lastReviewedAt;

}