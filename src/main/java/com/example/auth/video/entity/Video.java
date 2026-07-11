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
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Video {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String targetLang;
  private String title;
  private String author;
  private String videoId;
  private int duration;

  private Instant lastOpenedAt;
  private int lastPositionSeconds;

  @ManyToOne
  private AppUser user;

  public void updatePosition(int seconds) {
    this.lastPositionSeconds = seconds;
  }
  public void updateLastOpenedAt() {
    this.lastOpenedAt = Instant.now();
  }
}
