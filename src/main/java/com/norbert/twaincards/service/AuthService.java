package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.AuthDTO.AuthResponse;
import com.norbert.twaincards.dto.AuthDTO.LoginRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.InvalidPasswordException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервіс для автентифікації та авторизації
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final JwtUtils jwtUtils;
  private final AuthenticationManager authenticationManager;

  /**
   * Реєстрація нового користувача
   * @param registerRequest дані для реєстрації
   * @return відповідь з токеном
   */
  @Transactional
  public AuthResponse register(RegisterRequest registerRequest) {
    log.debug("Registering new user with username: {}", registerRequest.getUsername());

    // Реєструємо нового користувача
    var userDTO = userService.registerUser(registerRequest);

    // Отримуємо зареєстрованого користувача
    User user = userRepository.findById(userDTO.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found after registration"));

    // Створюємо JWT токен
    String token = jwtUtils.generateToken(
            org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(user.getRole().name())
                    .build(),
            user.getId(),
            user.getRole().name()
    );

    // Оновлюємо дату останнього входу
    user.setLastLoginDate(LocalDateTime.now());
    userRepository.save(user);

    // Створюємо відповідь
    return AuthResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .message("User registered successfully")
            .build();
  }

  /**
   * Вхід в систему
   * @param loginRequest дані для входу
   * @return відповідь з токеном
   */
  @Transactional
  public AuthResponse login(LoginRequest loginRequest) {
    log.debug("Authenticating user: {}", loginRequest.getUsernameOrEmail());

    try {
      // Виконуємо автентифікацію
      Authentication authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                      loginRequest.getUsernameOrEmail(),
                      loginRequest.getPassword()
              )
      );

      // Отримуємо користувача за ім'ям користувача або електронною поштою
      User user = userRepository.findByUsernameOrEmailWithNativeLanguage(loginRequest.getUsernameOrEmail())
              .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      // Створюємо JWT токен
      String token = jwtUtils.generateToken(
              (UserDetails) authentication.getPrincipal(),
              user.getId(),
              user.getRole().name()
      );

      // Оновлюємо дату останнього входу
      user.setLastLoginDate(LocalDateTime.now());
      userRepository.save(user);

      // Створюємо відповідь
      return AuthResponse.builder()
              .userId(user.getId())
              .username(user.getUsername())
              .email(user.getEmail())
              .role(user.getRole().name())
              .message("User authenticated successfully")
              .build();
    } catch (BadCredentialsException e) {
      log.error("Invalid credentials for user: {}", loginRequest.getUsernameOrEmail());
      throw new InvalidPasswordException("Invalid username/email or password");
    }
  }

  /**
   * Вихід з системи
   * @param userId ідентифікатор користувача
   */
  @Transactional
  public void logout(Long userId) {
    log.debug("Logging out user with id: {}", userId);
    // Фактично, для JWT немає потреби виконувати дії на сервері під час виходу,
    // оскільки токен зберігається на стороні клієнта і його просто потрібно видалити.
    // Однак, ми можемо зберігати інформацію про вихід для аналітики.
  }

  /**
   * Перевірка статусу автентифікації
   * @param userId ідентифікатор користувача
   * @return відповідь з даними користувача
   */
  @Transactional(readOnly = true)
  public AuthResponse checkAuthStatus(Long userId) {
    log.debug("Checking auth status for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    return AuthResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .message("User is authenticated")
            .build();
  }

  /**
   * Отримання нового токена доступу
   * @param userId ідентифікатор користувача
   * @return відповідь з новим токеном
   */
  @Transactional
  public AuthResponse refreshToken(Long userId) {
    log.debug("Refreshing token for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Створюємо JWT токен
    String token = jwtUtils.generateToken(
            org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(user.getRole().name())
                    .build(),
            user.getId(),
            user.getRole().name()
    );

    return AuthResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .message("Token refreshed successfully")
            .build();
  }
}