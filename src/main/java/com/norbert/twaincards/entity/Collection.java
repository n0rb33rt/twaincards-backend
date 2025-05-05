package com.norbert.twaincards.entity;

import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


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

  @Column(name = "users_count")
  private Integer usersCount;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<Card> cards = new HashSet<>();

  @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private Set<CollectionUserUsage> userUsages = new HashSet<>();

}