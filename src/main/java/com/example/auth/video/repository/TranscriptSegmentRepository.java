package com.example.auth.video.repository;

import com.example.auth.video.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, UUID> {
  @Query("""
      SELECT s.id FROM TranscriptSegment s
      WHERE s.video.id = :videoId
        AND s.tokens IS EMPTY
      ORDER BY s.start
      """)
  List<UUID> findIdsWithoutTokensByVideoId(@Param("videoId") UUID videoId);

  @Query("""
      SELECT DISTINCT s FROM TranscriptSegment s
      LEFT JOIN FETCH s.tokens
      WHERE s.id = :segmentId
      """)
  Optional<TranscriptSegment> findByIdWithTokens(@Param("segmentId") UUID segmentId);
}
