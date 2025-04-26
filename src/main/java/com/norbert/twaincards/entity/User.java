package com.norbert.twaincards.entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сутність, що представляє користувача в системі
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "email", nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "first_name", length = 50)
  private String firstName;

  @Column(name = "last_name", length = 50)
  private String lastName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "native_language_id")
  private Language nativeLanguage;

  @CreationTimestamp
  @Column(name = "registration_date", updatable = false)
  private LocalDateTime registrationDate;

  @Column(name = "last_login_date")
  private LocalDateTime lastLoginDate;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "role", length = 20)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<Collection> collections = new HashSet<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<LearningProgress> learningProgress = new HashSet<>();

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private UserStatistics statistics;

  /**
   * Типи ролей користувачів у системі
   */
  public enum UserRole {
    USER,
    PREMIUM,
    ADMIN
  }
}
