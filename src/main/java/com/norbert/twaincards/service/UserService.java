package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.AuthDTO.PasswordChangeRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.enumeration.UserRole;
import com.norbert.twaincards.exception.InvalidPasswordException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.exception.UserAlreadyExistsException;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public List<UserDTO> getAllUsers() {
    return userRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  public UserDTO getCurrentUser() {
    return convertToDto(securityUtils.getCurrentUser());
  }

  @Transactional(readOnly = true)
  public UserDTO getUserById(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    return convertToDto(user);
  }


  @Transactional
  public UserDTO registerUser(RegisterRequest registerRequest) {
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new UserAlreadyExistsException("Username already taken: " + registerRequest.getUsername());
    }

    if (userRepository.existsByEmail(registerRequest.getEmail())) {
      throw new UserAlreadyExistsException("Email already in use: " + registerRequest.getEmail());
    }

    User user = User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .isActive(false)
            .role(UserRole.USER)
            .registrationDate(LocalDateTime.now())
            .build();


    User savedUser = userRepository.save(user);
    log.info("User registered successfully: {}", savedUser.getUsername());

    return convertToDto(savedUser);
  }

  @Transactional
  public void changePassword(PasswordChangeRequest passwordChangeRequest) {
    User user = securityUtils.getCurrentUser();

    if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPasswordHash())) {
      throw new InvalidPasswordException("Current password is incorrect");
    }

    if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmPassword())) {
      throw new InvalidPasswordException("New password and confirmation do not match");
    }

    user.setPasswordHash(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
    userRepository.save(user);
    log.info("Password changed successfully for user: {}", user.getUsername());
  }


  private UserDTO convertToDto(User user) {
    UserDTO userDTO = modelMapper.map(user, UserDTO.class);


    if (user.getStatistics() != null) {
      userDTO.setTotalCards(user.getStatistics().getTotalCards());
      userDTO.setLearnedCards(user.getStatistics().getLearnedCards());
      userDTO.setLearningStreakDays(user.getStatistics().getLearningStreakDays());
      userDTO.setCompletionPercentage(user.getStatistics().getCompletionPercentage());
    }

    return userDTO;
  }
}