package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.Token;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.enumeration.TokenType;
import com.norbert.twaincards.exception.InvalidTokenException;
import com.norbert.twaincards.model.request.BuildingEmailMessageRequest;
import com.norbert.twaincards.model.request.SendingEmailRequest;
import com.norbert.twaincards.repository.TokenRepository;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.EmailBuilder;
import com.norbert.twaincards.util.EmailConfirmationHtmlUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.norbert.twaincards.util.LocalConstants.FRONT_BASE_URL;

@Service
@AllArgsConstructor
@Slf4j
public class TokenService {
    private final TokenRepository tokenRepository;
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
        Token token = buildEmailConfirmationToken(user, tokenString);
        tokenRepository.save(token);
        String link = buildConfirmationLink(tokenString);
        BuildingEmailMessageRequest buildingEmailMessageRequest = new BuildingEmailMessageRequest(user.getUsername(), link);
        return EmailBuilder.buildEmailConfirmationMessage(buildingEmailMessageRequest);
    }

    private Token buildEmailConfirmationToken(User user, String tokenString) {
        return Token.builder()
                .token(tokenString)
                .tokenType(TokenType.EMAIL_CONFIRMATION)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();
    }

    private String buildConfirmationLink(String token) {
        return BASE_CONFIRMATION_URL + token;
    }

    @Transactional
    public String confirmAccount(String token) {
        log.info("Confirming account with token: {}", token);
        Token emailConfirmationToken = tokenRepository.findByTokenAndTokenType(token, TokenType.EMAIL_CONFIRMATION)
                .orElseThrow(() -> new InvalidTokenException("Confirmation link is not valid"));

        if (Boolean.TRUE.equals(emailConfirmationToken.getUsed())) {
            log.warn("Token already confirmed: {}", token);
            // We'll return a success message anyway to avoid confusion for the user
            return EmailConfirmationHtmlUtil.getConfirmationHtml(FRONT_BASE_URL + "/login");
        }

        if (emailConfirmationToken.isExpired()) {
            log.error("Token expired: {}", token);
            throw new InvalidTokenException("Confirmation link has expired");
        }

        emailConfirmationToken.setUsed(true);
        emailConfirmationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(emailConfirmationToken);

        User user = emailConfirmationToken.getUser();
        if (user != null) {
            user.setIsActive(true);
            userRepository.save(user);
            log.info("User account activated: {}", user.getUsername());
        } else {
            log.error("User not found for token: {}", token);
        }
        
        return EmailConfirmationHtmlUtil.getConfirmationHtml(FRONT_BASE_URL + "/login");
    }
} 