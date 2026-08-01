package com.example.auth.video.repository;

import com.example.auth.video.entity.TranscriptToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TranscriptTokenRepository extends JpaRepository<TranscriptToken, UUID> {

  @Query("""
      SELECT t FROM TranscriptToken t
      JOIN FETCH t.segment s
      WHERE s.video.id = :videoId
        AND t.lemma IN :lemmas
      """)
  List<TranscriptToken> findByVideoIdAndLemmaIn(
      @Param("videoId") UUID videoId,
      @Param("lemmas") Collection<String> lemmas
  );
}
