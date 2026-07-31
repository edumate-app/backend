package com.example.auth.video.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TranscriptSegment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 1000)
  private String nativeText;

  @Column(nullable = false, length = 1000)
  private String targetText;

  @Column(nullable = false)
  private Double start;

  @Column(nullable = false)
  private Double duration;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;
}
