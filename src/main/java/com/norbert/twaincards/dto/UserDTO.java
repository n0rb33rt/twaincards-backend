package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO для передачі даних користувача
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

  private Long id;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, and ._-")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  private String email;

  // Not using @NotBlank for password as it might not be provided during updates
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  private String password;

  @Size(max = 50, message = "First name must not exceed 50 characters")
  private String firstName;

  @Size(max = 50, message = "Last name must not exceed 50 characters")
  private String lastName;

  private Long nativeLanguageId;
  private String nativeLanguageName;

  private LocalDateTime registrationDate;
  private LocalDateTime lastLoginDate;

  private Boolean isActive;

  private String role;

  // Статистика користувача
  private Integer totalCards;
  private Integer learnedCards;
  private Integer learningStreakDays;
  private Double completionPercentage;

  private UserStatisticsDTO statistics;
}