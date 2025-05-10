package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.Token;
import com.norbert.twaincards.entity.enumeration.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    
    Optional<Token> findByTokenAndTokenType(String token, TokenType tokenType);
    
    Optional<Token> findByUserIdAndTokenTypeAndUsed(Long userId, TokenType tokenType, Boolean used);
} 