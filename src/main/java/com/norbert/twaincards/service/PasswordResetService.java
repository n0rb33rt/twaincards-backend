package com.norbert.twaincards.service;

import com.norbert.twaincards.entity.Token;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.entity.enumeration.TokenType;
import com.norbert.twaincards.exception.InvalidTokenException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.model.request.BuildingEmailMessageRequest;
import com.norbert.twaincards.model.request.SendingEmailRequest;
import com.norbert.twaincards.repository.TokenRepository;
import com.norbert.twaincards.repository.UserRepository;
import com.norbert.twaincards.util.EmailBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailSenderService emailSender;
    private final PasswordEncoder passwordEncoder;
    
    private static final String BASE_RESET_URL = "http://localhost:5173/reset-password?token=";
    
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Check if there's an existing active token and invalidate it
        tokenRepository.findByUserIdAndTokenTypeAndUsed(user.getId(), TokenType.PASSWORD_RESET, false)
                .ifPresent(existingToken -> {
                    existingToken.setUsed(true);
                    existingToken.setUsedAt(LocalDateTime.now());
                    tokenRepository.save(existingToken);
                    log.info("Invalidated existing password reset token for user: {}", user.getUsername());
                });
        
        // Create new token
        String tokenValue = UUID.randomUUID().toString();
        Token passwordResetToken = Token.builder()
                .token(tokenValue)
                .tokenType(TokenType.PASSWORD_RESET)
                .user(user)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        
        tokenRepository.save(passwordResetToken);
        
        sendPasswordResetEmail(user, tokenValue);
        
        log.info("Password reset requested for user: {}", user.getUsername());
    }
    
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        Token token = tokenRepository.findByTokenAndTokenType(tokenValue, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> {
                    log.error("Invalid password reset token: {}", tokenValue);
                    return new InvalidTokenException("Password reset token is invalid");
                });
        
        if (token.isExpired()) {
            log.error("Password reset token expired: {}", tokenValue);
            throw new InvalidTokenException("Password reset token has expired");
        }
        
        if (token.getUsed()) {
            log.error("Password reset token already used: {}", tokenValue);
            throw new InvalidTokenException("Password reset token has already been used");
        }
        
        User user = token.getUser();
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        
        log.info("Password reset successful for user: {}", user.getUsername());
    }
    
    private void sendPasswordResetEmail(User user, String token) {
        String resetLink = BASE_RESET_URL + token;
        BuildingEmailMessageRequest emailRequest = new BuildingEmailMessageRequest(
                user.getUsername(),
                resetLink
        );
        
        String message = EmailBuilder.buildPasswordResetMessage(emailRequest);
        
        SendingEmailRequest sendingRequest = new SendingEmailRequest(
                message,
                user.getEmail(),
                "Reset Your TwainCards Password"
        );
        
        emailSender.send(sendingRequest);
        log.info("Password reset email sent to: {}", user.getEmail());
    }
} 