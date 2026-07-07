package com.example.auth.video.entity;

import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  private String videoId;
  private String thumbnailUrl;
  @Enumerated(EnumType.STRING)
  private VideoType type;

  @ManyToOne
  private AppUser user;
}
