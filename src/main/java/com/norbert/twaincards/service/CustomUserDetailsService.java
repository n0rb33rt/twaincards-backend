package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Сервіс для завантаження даних користувача для Spring Security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Завантаження даних користувача за іменем користувача або електронною поштою
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    log.debug("Loading user details for: {}", usernameOrEmail);

    // Пошук користувача за іменем користувача або електронною поштою
    User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
            .orElseThrow(() -> {
              log.error("User not found with username or email: {}", usernameOrEmail);
              return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
            });

    // Перевірка, чи користувач активний
    if (!user.getIsActive()) {
      log.error("User is deactivated: {}", usernameOrEmail);
      throw new UsernameNotFoundException("User is deactivated: " + usernameOrEmail);
    }

    // Створення об'єкта UserDetails
    return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPasswordHash(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );
  }
}