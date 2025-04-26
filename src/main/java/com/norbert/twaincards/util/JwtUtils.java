package com.norbert.twaincards.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Утиліта для роботи з JWT токенами
 */
@Component
@Slf4j
public class JwtUtils {

  @Value("${jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction}")
  private String secretString;

  @Value("${jwt.expiration:86400000}")
  private long expiration; // за замовчуванням 24 години

  private SecretKey secretKey;

  @PostConstruct
  public void init() {
    secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Отримати ім'я користувача з токена
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Отримати дату закінчення терміну дії токена
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Отримати ідентифікатор користувача з токена
   */
  public Long extractUserId(String token) {
    final Claims claims = extractAllClaims(token);
    return Long.parseLong(claims.get("userId").toString());
  }

  /**
   * Отримати роль користувача з токена
   */
  public String extractRole(String token) {
    final Claims claims = extractAllClaims(token);
    return claims.get("role", String.class);
  }

  /**
   * Отримати заявку з токена
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Отримати всі заявки з токена
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
  }

  /**
   * Перевірити, чи токен прострочений
   */
  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Створити токен з додатковими заявками
   */
  public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
    return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
  }

  /**
   * Створити токен для користувача
   */
  public String generateToken(UserDetails userDetails, Long userId, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("role", role);
    return generateToken(userDetails, claims);
  }

  /**
   * Перевірити валідність токена
   */
  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }
}