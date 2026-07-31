package com.example.auth.auth.service;

import com.example.auth.auth.entity.RefreshToken;
import com.example.auth.auth.exception.InvalidRefreshTokenException;
import com.example.auth.auth.repository.RefreshTokenRepository;
import com.example.auth.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;

  public String createRefreshToken(AppUser user) {
    String refreshToken = generateRandomToken();

    RefreshToken token = RefreshToken.builder()
        .user(user)
        .token(refreshToken)
        .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(token);
    return refreshToken;
  }

  public RefreshToken verifyToken(String refreshToken) {
    return refreshTokenRepository
        .findValidByToken(refreshToken)
        .orElseThrow(InvalidRefreshTokenException::new);
  }

  @Transactional
  public String rotateToken(RefreshToken oldToken) {
    String newTokenValue = generateRandomToken();

    RefreshToken newToken = RefreshToken.builder()
        .user(oldToken.getUser())
        .token(newTokenValue)
        .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
        .build();

    oldToken.setRevokedAt(Instant.now());
    oldToken.setReplacedBy(newToken);

    refreshTokenRepository.save(oldToken);
    refreshTokenRepository.save(newToken);

    return newTokenValue;
  }


  private String generateRandomToken() {
    byte[] bytes = new byte[64];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
