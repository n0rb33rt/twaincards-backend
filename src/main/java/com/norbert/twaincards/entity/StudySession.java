package com.norbert.twaincards.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "study_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudySession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time")
  private LocalDateTime endTime;

  @Column(name = "cards_reviewed")
  @Builder.Default
  private Integer cardsReviewed = 0;

  @Column(name = "correct_answers")
  @Builder.Default
  private Integer correctAnswers = 0;

  @Column(name = "device_type", length = 50)
  private String deviceType;

  @Column(name = "platform", length = 50)
  private String platform;

  @Column(name = "session_duration_seconds")
  private Integer sessionDurationSeconds;

  @Column(name = "is_completed")
  @Builder.Default
  private Boolean isCompleted = false;

  @ManyToMany
  @JoinTable(
          name = "session_collections",
          joinColumns = @JoinColumn(name = "session_id"),
          inverseJoinColumns = @JoinColumn(name = "collection_id")
  )
  @Builder.Default
  private Set<Collection> studiedCollections = new HashSet<>();

  @OneToMany(mappedBy = "studySession", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<LearningHistory> learningHistoryEntries = new HashSet<>();

  /**
   * Calculate accuracy rate for this study session
   * @return Percentage of correct answers (0-100)
   */
  @Transient
  public Double getAccuracyRate() {
    if (cardsReviewed == 0) {
      return 0.0;
    }
    return (correctAnswers * 100.0) / cardsReviewed;
  }

  /**
   * Updates session metrics when completed
   */
  public void completeSession() {
    if (!isCompleted) {
      this.endTime = LocalDateTime.now();
      this.sessionDurationSeconds = calculateDurationInSeconds();
      this.isCompleted = true;
    }
  }

  private Integer calculateDurationInSeconds() {
    if (startTime == null || endTime == null) {
      return 0;
    }

    return (int) java.time.Duration.between(startTime, endTime).getSeconds();
  }
}