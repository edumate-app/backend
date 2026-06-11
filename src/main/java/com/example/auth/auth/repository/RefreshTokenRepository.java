package com.example.auth.auth.repository;

import com.example.auth.auth.entity.RefreshToken;
import com.example.auth.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.token = :token
          AND rt.revokedAt IS NULL
          AND rt.expiresAt > CURRENT_TIMESTAMP
      """)
  Optional<RefreshToken> findValidByToken(@Param("token") String token);

  @Modifying
  @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = CURRENT_TIMESTAMP
        WHERE rt.user = :user
          AND rt.revokedAt IS NULL
      """)
  void revokeAllForUser(@Param("user") AppUser user);

  @Modifying
  @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.expiresAt < :threshold
      """)
  void deleteExpired(@Param("threshold") Instant threshold);

  List<RefreshToken> findAllByUser(AppUser user);
}