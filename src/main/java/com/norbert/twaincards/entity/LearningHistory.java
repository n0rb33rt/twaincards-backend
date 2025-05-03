package com.norbert.twaincards.entity;

import com.norbert.twaincards.entity.enumeration.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

  @CreationTimestamp
  @Column(name = "performed_at", updatable = false)
  private LocalDateTime performedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "study_session_id")
  private StudySession studySession;
}