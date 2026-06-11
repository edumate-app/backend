package com.example.auth.auth.entity;

import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  private AppUser user;

  @Column(nullable = false, unique = true)
  public String token;

  private Instant expiresAt;
  private Instant revokedAt;

  @ManyToOne
  private RefreshToken replacedBy;
}
