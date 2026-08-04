package com.example.auth.video.repository;

import com.example.auth.video.entity.SegmentNativeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SegmentNativeTranslationRepository extends JpaRepository<SegmentNativeTranslation, UUID> {
  long countBySegment_Video_IdAndNativeLang(UUID videoId, String nativeLang);
  List<SegmentNativeTranslation> findBySegment_Video_IdAndNativeLang(UUID videoId, String nativeLang);
  Optional<SegmentNativeTranslation> findBySegment_IdAndNativeLang(UUID segmentId, String nativeLang);
  @Query("""
      SELECT t FROM SegmentNativeTranslation t
      WHERE t.segment.id IN :segmentIds
        AND t.nativeLang = :nativeLang
      """)
  List<SegmentNativeTranslation> findBySegmentIdInAndNativeLang(
      @Param("segmentIds") Collection<UUID> segmentIds,
      @Param("nativeLang") String nativeLang
  );
}
