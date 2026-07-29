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
  @Query("""
        select v.targetLang
        from Video v
        where v.videoId = :videoId
          and v.user = :user
    """)
  List<String> findTargetLangsByVideoIdAndUser(
      @Param("videoId") String videoId,
      @Param("user") AppUser user
  );

  List<Video> findTop10ByUserOrderByLastOpenedAtDesc(AppUser user);
  @Modifying
  @Query("UPDATE Video v SET v.lastOpenedAt = CURRENT_TIMESTAMP WHERE v.id = :videoId")
  int updateLastOpenedAt(@Param("videoId") UUID videoId);

  @Modifying
  @Query("UPDATE Video v SET v.lastPositionSeconds = :position, v.lastOpenedAt = CURRENT_TIMESTAMP WHERE v.id = :videoId")
  int updatePositionAndLastOpened(@Param("videoId") UUID videoId, @Param("position") int positionSeconds);

  @Query("SELECT v FROM Video v LEFT JOIN FETCH v.transcriptSegments ts WHERE v.id = :videoId ORDER BY ts.start")
  Optional<Video> findByIdWithSegments(@Param("videoId") UUID videoId);

  List<Video> findAllByUser(AppUser user);
}
