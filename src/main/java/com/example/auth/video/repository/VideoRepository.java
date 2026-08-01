package com.example.auth.video.repository;

import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

  Optional<Video> findByVideoIdAndTargetLang(String videoId, String targetLang);

  @Query("""
      SELECT v FROM Video v
      LEFT JOIN FETCH v.transcriptSegments ts
      WHERE v.id = :videoId
      ORDER BY ts.start
      """)
  Optional<Video> findByIdWithSegments(@Param("videoId") UUID videoId);
}
