package com.norbert.twaincards.dto;

import com.norbert.twaincards.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

  @NotBlank(message = "Ім'я користувача не може бути пустим")
  @Size(min = 3, max = 50, message = "Ім'я користувача повинно містити від 3 до 50 символів")
  private String username;

  @NotBlank(message = "Email не може бути пустим")
  @Email(message = "Некоректний формат email")
  private String email;

  @Size(max = 50, message = "Ім'я повинно містити не більше 50 символів")
  private String firstName;

  @Size(max = 50, message = "Прізвище повинно містити не більше 50 символів")
  private String lastName;

  private Long nativeLanguageId;
  private String nativeLanguageName;

  private LocalDateTime registrationDate;
  private LocalDateTime lastLoginDate;

  private Boolean isActive;
  private UserRole role;

  // Статистика користувача
  private Integer totalCards;
  private Integer learnedCards;
  private Integer learningStreakDays;
  private Double completionPercentage;
}