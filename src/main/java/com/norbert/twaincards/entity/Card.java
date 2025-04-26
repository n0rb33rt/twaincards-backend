package com.norbert.twaincards.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сутність, що представляє картку для вивчення
 */
@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  @Column(name = "phonetic_text")
  private String phoneticText;

  @Column(name = "example_usage")
  private String exampleUsage;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "audio_url")
  private String audioUrl;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<LearningProgress> learningProgress = new HashSet<>();

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<LearningHistory> learningHistory = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "card_tags", joinColumns = @JoinColumn(name = "card_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<Tag> tags = new HashSet<>();

  /**
   * Додати тег до картки
   *
   * @param tag тег для додавання
   */
  public void addTag(Tag tag) {
    this.tags.add(tag);
    tag.getCards().add(this);
  }

  /**
   * Видалити тег з картки
   *
   * @param tag тег для видалення
   */
  public void removeTag(Tag tag) {
    this.tags.remove(tag);
    tag.getCards().remove(this);
  }
}