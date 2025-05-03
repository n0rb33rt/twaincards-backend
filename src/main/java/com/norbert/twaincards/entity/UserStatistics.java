package com.norbert.twaincards.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_statistics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "total_cards")
  @Builder.Default
  private Integer totalCards = 0;

  @Column(name = "learned_cards")
  @Builder.Default
  private Integer learnedCards = 0;

  @Column(name = "learning_streak_days")
  @Builder.Default
  private Integer learningStreakDays = 0;

  @Column(name = "last_study_date")
  private LocalDate lastStudyDate;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Transient
  public Double getCompletionPercentage() {
    if (totalCards == 0) {
      return 0.0;
    }
    return (learnedCards * 100.0) / totalCards;
  }
}