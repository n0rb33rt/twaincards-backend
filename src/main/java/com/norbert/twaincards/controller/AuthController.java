package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.AuthDTO.AuthResponse;
import com.norbert.twaincards.dto.AuthDTO.LoginRequest;
import com.norbert.twaincards.dto.AuthDTO.RegisterRequest;
import com.norbert.twaincards.service.AuthService;
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

  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refreshToken() {
    AuthResponse response = authService.refreshToken();
    return ResponseEntity.ok(response);
  }
}