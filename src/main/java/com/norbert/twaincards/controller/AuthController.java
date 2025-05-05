package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.AuthDTO.AuthResponse;
import com.norbert.twaincards.dto.AuthDTO.LoginRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.model.request.PasswordResetRequest;
import com.norbert.twaincards.model.request.RequestPasswordResetRequest;
import com.norbert.twaincards.service.AuthService;
import com.norbert.twaincards.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
  private final AuthService authService;
  private final PasswordResetService passwordResetService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
    AuthResponse response = authService.register(registerRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
    AuthResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/request-password-reset")
  public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid RequestPasswordResetRequest request) {
    log.info("Request to reset password for email: {}", request.getEmail());
    passwordResetService.requestPasswordReset(request.getEmail());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
    log.info("Request to set new password with token");
    passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    return ResponseEntity.ok().build();
  }
}