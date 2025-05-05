package com.norbert.twaincards.controller;

import com.norbert.twaincards.dto.AuthDTO.PasswordChangeRequest;
import com.norbert.twaincards.dto.UserDTO;
import com.norbert.twaincards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<List<UserDTO>> getAllUsers() {
    log.info("Request to get all users");
    return ResponseEntity.ok(userService.getAllUsers());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
    log.info("Request to get user by id: {}", id);
    return ResponseEntity.ok(userService.getUserById(id));
  }


  @GetMapping("/me")
  public ResponseEntity<UserDTO> getCurrentUser() {
    log.info("Request to get current user");
    return ResponseEntity.ok(userService.getCurrentUser());
  }
  
  @PutMapping("/me")
  public ResponseEntity<UserDTO> updateCurrentUser(@RequestBody @Valid UserDTO userDTO) {
    log.info("Request to update current user");
    return ResponseEntity.ok(userService.updateCurrentUser(userDTO));
  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(@RequestBody @Valid PasswordChangeRequest passwordChangeRequest) {
    log.info("Request to change password");
    userService.changePassword(passwordChangeRequest);
    return ResponseEntity.ok().build();
  }

}