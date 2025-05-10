package com.norbert.twaincards.entity;

import com.norbert.twaincards.entity.enumeration.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(nullable = false)
    private Boolean used;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
} 