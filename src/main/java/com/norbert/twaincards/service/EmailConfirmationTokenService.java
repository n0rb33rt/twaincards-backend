package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.EmailConfirmationToken;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.InvalidTokenException;
import com.norbert.twaincards.model.request.BuildingEmailMessageRequest;
import com.norbert.twaincards.model.request.SendingEmailRequest;
import com.norbert.twaincards.repository.EmailConfirmationTokenRepository;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.EmailBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class EmailConfirmationTokenService {
  private final EmailConfirmationTokenRepository confirmationTokenRepository;
  private final UserRepository userRepository;
  private final EmailSenderService emailSender;

  private final static String BASE_CONFIRMATION_URL = "http://localhost:8080/api/v1/confirm?token=";

  public void sendConfirmationMail(User user) {
    String message = buildSendingMessage(user);
    SendingEmailRequest sendingEmailRequest = new SendingEmailRequest(message, user.getEmail(), "Confirm your account");
    emailSender.send(sendingEmailRequest);
  }

    private String buildSendingMessage(User user) {
      String tokenString = UUID.randomUUID().toString();
      EmailConfirmationToken token = buildEmailConfirmationToken(user, tokenString);
      confirmationTokenRepository.save(token);
      String link = buildConfirmationLink(tokenString);
      BuildingEmailMessageRequest buildingEmailMessageRequest = new BuildingEmailMessageRequest(user.getUsername(), link);
      return EmailBuilder.buildEmailConfirmationMessage(buildingEmailMessageRequest);
  }

  private EmailConfirmationToken buildEmailConfirmationToken(User user, String tokenString) {
    return EmailConfirmationToken
            .builder()
            .token(tokenString)
            .confirmed(false)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .user(user)
            .build();
  }

  private String buildConfirmationLink(String token) {
    return BASE_CONFIRMATION_URL + token;
  }

  @Transactional
  public void confirmAccount(String token) {
    log.info("Confirming account with token: {}", token);
    EmailConfirmationToken emailConfirmationToken = confirmationTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Confirmation link is not valid"));


    if (Boolean.TRUE.equals(emailConfirmationToken.getConfirmed())) {
      log.warn("Token already confirmed: {}", token);
      return;
    }

    if (emailConfirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      log.error("Token expired: {}", token);
      throw new InvalidTokenException("Confirmation link has expired");
    }

    emailConfirmationToken.setConfirmed(true);
    emailConfirmationToken.setConfirmedAt(LocalDateTime.now());
    confirmationTokenRepository.save(emailConfirmationToken);

    User user = emailConfirmationToken.getUser();
    if (user != null) {
      user.setIsActive(true);
      userRepository.save(user);
      log.info("User account activated: {}", user.getUsername());
    } else {
      log.error("User not found for token: {}", token);
    }
  }

}