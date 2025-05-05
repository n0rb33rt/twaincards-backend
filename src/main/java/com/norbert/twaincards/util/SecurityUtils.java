package com.norbert.twaincards.util;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

  private final UserRepository userRepository;

  /**
   * Check if a user is currently authenticated
   * 
   * @return true if a user is authenticated, false otherwise
   */
  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && 
           authentication.isAuthenticated() && 
           !"anonymousUser".equals(authentication.getPrincipal());
  }

  /**
   * Get the currently logged in user
   * 
   * @return User entity for the authenticated user
   * @throws ResourceNotFoundException if the user is not found
   */
  public User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
  }

  public Long getCurrentUserId() {
    return getCurrentUser().getId();
  }
}