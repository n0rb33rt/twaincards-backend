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


@Component
@Slf4j
public class  JwtUtils {

  @Value("${jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction}")
  private String secretString;

  @Value("${jwt.expiration:86400000}")
  private long expiration; // за замовчуванням 24 години

  private SecretKey secretKey;

  @PostConstruct
  public void init() {
    secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
  }


  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }


  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }


  public Long extractUserId(String token) {
    final Claims claims = extractAllClaims(token);
    return Long.parseLong(claims.get("userId").toString());
  }

  public String extractRole(String token) {
    final Claims claims = extractAllClaims(token);
    return claims.get("role", String.class);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
  }


  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
    return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
  }

  public String generateToken(UserDetails userDetails, Long userId, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("role", role);
    return generateToken(userDetails, claims);
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }
}