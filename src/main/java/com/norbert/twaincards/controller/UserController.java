package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.dto.AuthDTO.PasswordChangeRequest;
import com.norbert.twaincards.service.UserActivityLogService;
import com.norbert.twaincards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контролер для управління користувачами
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final UserActivityLogService activityLogService;

  /**
   * Отримати всіх користувачів (лише для адміністраторів)
   */
  @GetMapping
  public ResponseEntity<List<UserDTO>> getAllUsers() {
    log.info("Request to get all users");
    return ResponseEntity.ok(userService.getAllUsers());
  }

  /**
   * Отримати користувача за ідентифікатором
   */
  @GetMapping("/{id}")
  public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
    log.info("Request to get user by id: {}", id);
    return ResponseEntity.ok(userService.getUserById(id));
  }

  /**
   * Отримати поточного користувача
   */
  @GetMapping("/me")
  public ResponseEntity<UserDTO> getCurrentUser(@RequestAttribute("userId") Long userId) {
    log.info("Request to get current user");
    return ResponseEntity.ok(userService.getUserById(userId));
  }

  /**
   * Зареєструвати нового користувача
   */
  @PostMapping("/register")
  public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
    log.info("Request to register new user with username: {}", registerRequest.getUsername());
    UserDTO createdUser = userService.registerUser(registerRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            createdUser.getId(),
            "REGISTER",
            "USER",
            createdUser.getId(),
            "User registered"
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  /**
   * Оновити дані користувача
   */
  @PutMapping("/{id}")
  public ResponseEntity<UserDTO> updateUser(
          @PathVariable Long id,
          @RequestBody @Valid UserDTO userDTO,
          @RequestAttribute("userId") Long currentUserId) {

    // Перевірка, що користувач оновлює свій профіль або є адміністратором
    if (!id.equals(currentUserId)) {
      log.warn("User {} attempted to update user {}", currentUserId, id);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    log.info("Request to update user with id: {}", id);
    UserDTO updatedUser = userService.updateUser(id, userDTO);

    // Логуємо активність
    activityLogService.logUserActivity(
            currentUserId,
            "UPDATE",
            "USER",
            id,
            "User profile updated"
    );

    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Змінити пароль користувача
   */
  @PostMapping("/{id}/change-password")
  public ResponseEntity<Void> changePassword(
          @PathVariable Long id,
          @RequestBody @Valid PasswordChangeRequest passwordChangeRequest,
          @RequestAttribute("userId") Long currentUserId) {

    // Перевірка, що користувач змінює свій пароль
    if (!id.equals(currentUserId)) {
      log.warn("User {} attempted to change password for user {}", currentUserId, id);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    log.info("Request to change password for user with id: {}", id);
    userService.changePassword(id, passwordChangeRequest);

    // Логуємо активність
    activityLogService.logUserActivity(
            currentUserId,
            "CHANGE_PASSWORD",
            "USER",
            id,
            "User password changed"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Деактивувати користувача (лише для адміністраторів)
   */
  @PostMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateUser(
          @PathVariable Long id,
          @RequestAttribute("userId") Long currentUserId) {

    log.info("Request to deactivate user with id: {}", id);
    userService.deactivateUser(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            currentUserId,
            "DEACTIVATE",
            "USER",
            id,
            "User deactivated"
    );

    return ResponseEntity.ok().build();
  }

  /**
   * Активувати користувача (лише для адміністраторів)
   */
  @PostMapping("/{id}/activate")
  public ResponseEntity<Void> activateUser(
          @PathVariable Long id,
          @RequestAttribute("userId") Long currentUserId) {

    log.info("Request to activate user with id: {}", id);
    userService.activateUser(id);

    // Логуємо активність
    activityLogService.logUserActivity(
            currentUserId,
            "ACTIVATE",
            "USER",
            id,
            "User activated"
    );

    return ResponseEntity.ok().build();
  }
}