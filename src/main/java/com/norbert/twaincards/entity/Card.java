package com.norbert.twaincards.entity;

import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"collection", "learningProgress", "learningHistory"})
public class Card {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collection_id", nullable = false)
  private Collection collection;

  @Column(name = "front_text", nullable = false)
  private String frontText;

  @Column(name = "back_text", nullable = false)
  private String backText;

  @Column(name = "example_usage")
  private String exampleUsage;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<LearningProgress> learningProgress = new HashSet<>();

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<LearningHistory> learningHistory = new HashSet<>();
}