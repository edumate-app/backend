package com.example.auth.video.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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
  private String targetText;

  @Column(nullable = false)
  private Double start;

  @Column(nullable = false)
  private Double duration;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  @OneToMany(
      mappedBy = "segment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @OrderBy("tokenIndex ASC")
  @Builder.Default
  private List<TranscriptToken> tokens = new ArrayList<>();

  @OneToMany(
      mappedBy = "segment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  @Builder.Default
  private List<SegmentNativeTranslation> nativeTranslations = new ArrayList<>();

  public void addToken(TranscriptToken token) {
    tokens.add(token);
    token.setSegment(this);
  }

  public void addTokens(List<TranscriptToken> tokensToAdd) {
    if (tokensToAdd == null || tokensToAdd.isEmpty()) {
      return;
    }
    tokensToAdd.forEach(this::addToken);
  }

  public void addNativeTranslation(SegmentNativeTranslation translation) {
    nativeTranslations.add(translation);
    translation.setSegment(this);
  }
}
