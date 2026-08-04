package com.example.auth.video.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Table(
    name = "segment_native_translation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_segment_native_translation_segment_lang",
        columnNames = {"segment_id", "native_lang"}
    ),
    indexes = {
        @Index(name = "idx_segment_native_translation_lang", columnList = "native_lang")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentNativeTranslation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "segment_id", nullable = false)
  private TranscriptSegment segment;
  @Column(name = "native_lang", nullable = false, length = 16)
  private String nativeLang;
  @Column(nullable = false, length = 1000)
  private String nativeText;
}