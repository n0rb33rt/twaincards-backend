package com.norbert.twaincards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class AuthDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterRequest {

    @NotBlank(message = "Ім'я користувача не може бути пустим")
    @Size(min = 3, max = 50, message = "Ім'я користувача повинно містити від 3 до 50 символів")
    private String username;

    @NotBlank(message = "Email не може бути пустим")
    @Email(message = "Некоректний формат email")
    private String email;

    @NotBlank(message = "Пароль не може бути пустим")
    @Size(min = 6, message = "Пароль повинен містити не менше 6 символів")
    private String password;

    @Size(max = 50, message = "Ім'я повинно містити не більше 50 символів")
    private String firstName;

    @Size(max = 50, message = "Прізвище повинно містити не більше 50 символів")
    private String lastName;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoginRequest {

    @NotBlank(message = "Ім'я користувача або email не може бути пустим")
    private String usernameOrEmail;

    @NotBlank(message = "Пароль не може бути пустим")
    private String password;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AuthResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private String token;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PasswordChangeRequest {

    @NotBlank(message = "Поточний пароль не може бути пустим")
    private String currentPassword;

    @NotBlank(message = "Новий пароль не може бути пустим")
    @Size(min = 6, message = "Новий пароль повинен містити не менше 6 символів")
    private String newPassword;

    @NotBlank(message = "Підтвердження пароля не може бути пустим")
    private String confirmPassword;
  }
}