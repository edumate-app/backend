package com.example.auth.video.entity;

import com.example.auth.expression.dto.NumberType;
import com.example.auth.expression.dto.PosType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "transcript_token",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_transcript_token_segment_index",
        columnNames = {"segment_id", "token_index"}
    ),
    indexes = {
        @Index(name = "idx_transcript_token_lemma", columnList = "lemma"),
        @Index(name = "idx_transcript_token_segment_lemma", columnList = "segment_id, lemma")
    }
)
public class TranscriptToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "segment_id", nullable = false)
  private TranscriptSegment segment;

  @Column(name = "token_index", nullable = false)
  private Integer tokenIndex;

  @Column(nullable = false)
  private String text;

  @Column(nullable = false)
  private String lemma;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PosType pos;

  private Integer person;

  @Enumerated(EnumType.STRING)
  private NumberType number;

  private String tense;

  private String mood;

  private String gender;
}
