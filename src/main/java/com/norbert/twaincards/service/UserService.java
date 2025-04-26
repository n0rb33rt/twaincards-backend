package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.dto.AuthDTO.PasswordChangeRequest;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UserAlreadyExistsException;
import com.norbert.twaincards.exception.InvalidPasswordException;
import com.norbert.twaincards.repository.LanguageRepository;
import com.norbert.twaincards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з користувачами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final LanguageRepository languageRepository;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;

  /**
   * Отримати всіх користувачів
   * @return список DTO користувачів
   */
  @Transactional(readOnly = true)
  public List<UserDTO> getAllUsers() {
    log.debug("Getting all users");
    return userRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  /**
   * Отримати користувача за ідентифікатором
   * @param id ідентифікатор користувача
   * @return DTO користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public UserDTO getUserById(Long id) {
    log.debug("Getting user by id: {}", id);
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    return convertToDto(user);
  }

  /**
   * Отримати користувача за іменем
   * @param username ім'я користувача
   * @return DTO користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional(readOnly = true)
  public UserDTO getUserByUsername(String username) {
    log.debug("Getting user by username: {}", username);
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    return convertToDto(user);
  }

  /**
   * Створити нового користувача
   * @param registerRequest дані для реєстрації
   * @return DTO створеного користувача
   * @throws UserAlreadyExistsException якщо користувач з таким іменем або email вже існує
   */
  @Transactional
  public UserDTO registerUser(RegisterRequest registerRequest) {
    log.debug("Registering new user with username: {}", registerRequest.getUsername());

    // Перевірка існування користувача з таким іменем або email
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new UserAlreadyExistsException("Username already taken: " + registerRequest.getUsername());
    }

    if (userRepository.existsByEmail(registerRequest.getEmail())) {
      throw new UserAlreadyExistsException("Email already in use: " + registerRequest.getEmail());
    }

    // Створення нового користувача
    User user = User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .isActive(true)
            .role(User.UserRole.USER)
            .registrationDate(LocalDateTime.now())
            .build();

    // Додавання рідної мови, якщо вказана
    if (registerRequest.getNativeLanguageId() != null) {
      languageRepository.findById(registerRequest.getNativeLanguageId())
              .ifPresent(user::setNativeLanguage);
    }

    User savedUser = userRepository.save(user);
    log.info("User registered successfully: {}", savedUser.getUsername());

    return convertToDto(savedUser);
  }

  /**
   * Оновити дані користувача
   * @param id ідентифікатор користувача
   * @param userDTO нові дані користувача
   * @return оновлений DTO користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional
  public UserDTO updateUser(Long id, UserDTO userDTO) {
    log.debug("Updating user with id: {}", id);
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    // Оновлення основних даних
    user.setFirstName(userDTO.getFirstName());
    user.setLastName(userDTO.getLastName());

    // Оновлення рідної мови, якщо вказана
    if (userDTO.getNativeLanguageId() != null) {
      languageRepository.findById(userDTO.getNativeLanguageId())
              .ifPresent(user::setNativeLanguage);
    }

    User updatedUser = userRepository.save(user);
    log.info("User updated successfully: {}", updatedUser.getUsername());

    return convertToDto(updatedUser);
  }

  /**
   * Змінити пароль користувача
   * @param userId ідентифікатор користувача
   * @param passwordChangeRequest дані для зміни пароля
   * @throws ResourceNotFoundException якщо користувача не знайдено
   * @throws InvalidPasswordException якщо поточний пароль невірний або новий пароль не співпадає з підтвердженням
   */
  @Transactional
  public void changePassword(Long userId, PasswordChangeRequest passwordChangeRequest) {
    log.debug("Changing password for user with id: {}", userId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Перевірка поточного пароля
    if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPasswordHash())) {
      throw new InvalidPasswordException("Current password is incorrect");
    }

    // Перевірка співпадіння нового пароля з підтвердженням
    if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmPassword())) {
      throw new InvalidPasswordException("New password and confirmation do not match");
    }

    // Зміна пароля
    user.setPasswordHash(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
    userRepository.save(user);
    log.info("Password changed successfully for user: {}", user.getUsername());
  }

  /**
   * Деактивувати користувача
   * @param id ідентифікатор користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional
  public void deactivateUser(Long id) {
    log.debug("Deactivating user with id: {}", id);
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    user.setIsActive(false);
    userRepository.save(user);
    log.info("User deactivated successfully: {}", user.getUsername());
  }

  /**
   * Активувати користувача
   * @param id ідентифікатор користувача
   * @throws ResourceNotFoundException якщо користувача не знайдено
   */
  @Transactional
  public void activateUser(Long id) {
    log.debug("Activating user with id: {}", id);
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    user.setIsActive(true);
    userRepository.save(user);
    log.info("User activated successfully: {}", user.getUsername());
  }

  /**
   * Оновити дату останнього входу користувача
   * @param id ідентифікатор користувача
   */
  @Transactional
  public void updateLastLoginDate(Long id) {
    log.debug("Updating last login date for user with id: {}", id);
    userRepository.findById(id).ifPresent(user -> {
      user.setLastLoginDate(LocalDateTime.now());
      userRepository.save(user);
    });
  }

  /**
   * Конвертувати сутність користувача в DTO
   * @param user сутність користувача
   * @return DTO користувача
   */
  private UserDTO convertToDto(User user) {
    UserDTO userDTO = modelMapper.map(user, UserDTO.class);

    // Додавання інформації про рідну мову
    if (user.getNativeLanguage() != null) {
      userDTO.setNativeLanguageId(user.getNativeLanguage().getId());
      userDTO.setNativeLanguageName(user.getNativeLanguage().getName());
    }

    // Додавання статистики, якщо доступна
    if (user.getStatistics() != null) {
      userDTO.setTotalCards(user.getStatistics().getTotalCards());
      userDTO.setLearnedCards(user.getStatistics().getLearnedCards());
      userDTO.setLearningStreakDays(user.getStatistics().getLearningStreakDays());
      userDTO.setCompletionPercentage(user.getStatistics().getCompletionPercentage());
    }

    return userDTO;
  }
}