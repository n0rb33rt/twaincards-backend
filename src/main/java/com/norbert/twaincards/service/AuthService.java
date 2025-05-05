package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.AuthDTO.AuthResponse;
import com.norbert.twaincards.dto.AuthDTO.LoginRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.InvalidPasswordException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.JwtUtils;
import com.norbert.twaincards.util.SecurityUtils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final SecurityUtils securityUtils;
  private final UserService userService;
  private final JwtUtils jwtUtils;
  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;

  public AuthResponse register(RegisterRequest registerRequest) {
    UserDTO userDTO = userService.registerUser(registerRequest);

    User user = userRepository.findById(userDTO.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found after registration"));

    tokenService.sendConfirmationMail(user);

    return AuthResponse.builder()
            .message("Registration successful! Please check your email to activate your account.")
            .build();
  }

  public AuthResponse login(LoginRequest loginRequest) {
    try {
      Authentication authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                      loginRequest.getUsernameOrEmail(),
                      loginRequest.getPassword()
              )
      );

      User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail())
              .orElseThrow(() -> new ResourceNotFoundException("User not found"));

      String token = jwtUtils.generateToken(
              (UserDetails) authentication.getPrincipal(),
              user.getId(),
              user.getRole().getName()
      );

      user.setLastLoginDate(LocalDateTime.now());
      userRepository.save(user);

      return AuthResponse.builder()
              .userId(user.getId())
              .username(user.getUsername())
              .email(user.getEmail())
              .role(user.getRole().getName())
              .token(token)
              .message("User authenticated successfully")
              .build();
    } catch (BadCredentialsException e) {
      log.error("Invalid credentials for user: {}", loginRequest.getUsernameOrEmail());
      throw new InvalidPasswordException("Invalid username/email or password");
    }
  }
}