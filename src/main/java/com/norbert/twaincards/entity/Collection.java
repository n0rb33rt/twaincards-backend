package com.norbert.twaincards.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сутність, що представляє колекцію карток для вивчення
 */
@Entity
@Table(name = "collections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_language_id", nullable = false)
  private Language sourceLanguage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_language_id", nullable = false)
  private Language targetLanguage;

  @Column(name = "is_public")
  private Boolean isPublic;

  @Column(name = "is_default")
  private Boolean isDefault;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<Card> cards = new HashSet<>();

  /**
   * Додати картку до колекції
   *
   * @param card картка для додавання
   * @return додана картка
   */
  public Card addCard(Card card) {
    cards.add(card);
    card.setCollection(this);
    return card;
  }

  /**
   * Видалити картку з колекції
   *
   * @param card картка для видалення
   */
  public void removeCard(Card card) {
    cards.remove(card);
    card.setCollection(null);
  }
}