package com.example.auth.video.entity;

import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_video",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVideo {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  private Instant lastOpenedAt;
  private int lastPositionSeconds;
}
