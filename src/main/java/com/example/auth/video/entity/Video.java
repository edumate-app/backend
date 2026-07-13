package com.example.auth.video.entity;

import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

  @OneToMany(
      mappedBy = "video",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @OrderBy("start ASC")
  @Builder.Default
  private List<TranscriptSegment> transcriptSegments = new ArrayList<>();

  public void updatePosition(int seconds) {
    this.lastPositionSeconds = seconds;
  }
  public void updateLastOpenedAt() {
    this.lastOpenedAt = Instant.now();
  }
  private void addTranscriptSegment(TranscriptSegment segment) {
    transcriptSegments.add(segment);
    segment.setVideo(this);
  }
  public void addTranscriptSegments(List<TranscriptSegment> segments) {
    if (segments != null && !segments.isEmpty()) {
      segments.forEach(this::addTranscriptSegment);
    }
  }
}
