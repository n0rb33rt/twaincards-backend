package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.AuthDTO.AuthResponse;
import com.norbert.twaincards.dto.AuthDTO.LoginRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.service.AuthService;
import com.norbert.twaincards.service.UserActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контролер для автентифікації та авторизації
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthService authService;
  private final UserActivityLogService activityLogService;

  /**
   * Реєстрація нового користувача
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
    log.info("Request to register new user with username: {}", registerRequest.getUsername());
    AuthResponse response = authService.register(registerRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            response.getUserId(),
            "REGISTER",
            "USER",
            response.getUserId(),
            "User registered"
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Вхід в систему
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
    log.info("Request to login user: {}", loginRequest.getUsernameOrEmail());
    AuthResponse response = authService.login(loginRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            response.getUserId(),
            "LOGIN",
            "USER",
            response.getUserId(),
            "User logged in"
    );

    return ResponseEntity.ok(response);
  }

  /**
   * Вихід з системи
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestAttribute("userId") Long userId) {
    log.info("Request to logout user with id: {}", userId);
    authService.logout(userId);

    // Логуємо активність
    activityLogService.logUserActivity(
            userId,
            "LOGOUT",
            "USER",
            userId,
            "User logged out"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Перевірка статусу автентифікації
   */
  @GetMapping("/status")
  public ResponseEntity<AuthResponse> checkAuthStatus(@RequestAttribute("userId") Long userId) {
    log.info("Request to check auth status for user with id: {}", userId);
    AuthResponse response = authService.checkAuthStatus(userId);
    return ResponseEntity.ok(response);
  }

  /**
   * Отримання нового токена доступу
   */
  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refreshToken(@RequestAttribute("userId") Long userId) {
    log.info("Request to refresh token for user with id: {}", userId);
    AuthResponse response = authService.refreshToken(userId);
    return ResponseEntity.ok(response);
  }
}