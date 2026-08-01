package com.example.auth.expression.entity;

import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.Video;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_expression_context_expression_segment",
        columnNames = {"expression_id", "transcript_segment_id"}
    )
)
public class ExpressionContext {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "expression_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Expression expression;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transcript_segment_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TranscriptSegment transcriptSegment;

  /** Snapshot of segment text at save time (stable if subtitles change later). */
  private String targetSentence;
  private String nativeTranslation;

  @ElementCollection
  @CollectionTable(
      name = "expression_context_forms",
      joinColumns = @JoinColumn(name = "expression_context_id")
  )
  @Column(name = "form")
  @Builder.Default
  private List<String> matchedForms = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Video video;

  @Column(nullable = false)
  private Double startSeconds;

  @CreationTimestamp
  @Column(name = "saved_at", updatable = false, nullable = false)
  private LocalDateTime savedAt;
}
