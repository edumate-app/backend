package com.example.auth.video.repository;

import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.UserVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserVideoRepository extends JpaRepository<UserVideo, UUID> {

  Optional<UserVideo> findByUserAndVideo_Id(AppUser user, UUID videoId);

  @Query("""
      select uv.video.targetLang
      from UserVideo uv
      where uv.video.videoId = :videoId
        and uv.user = :user
      """)
  List<String> findTargetLangsByVideoIdAndUser(
      @Param("videoId") String videoId,
      @Param("user") AppUser user
  );

  @Query("""
      select uv from UserVideo uv
      join fetch uv.video v
      where uv.user = :user
      order by uv.lastOpenedAt desc
      """)
  List<UserVideo> findTop10ByUserOrderByLastOpenedAtDesc(@Param("user") AppUser user);

  @Modifying
  @Query("""
      UPDATE UserVideo uv
      SET uv.lastOpenedAt = CURRENT_TIMESTAMP
      WHERE uv.video.id = :videoId AND uv.user = :user
      """)
  int updateLastOpenedAt(@Param("videoId") UUID videoId, @Param("user") AppUser user);

  @Modifying
  @Query("""
      UPDATE UserVideo uv
      SET uv.lastPositionSeconds = :position,
          uv.lastOpenedAt = CURRENT_TIMESTAMP
      WHERE uv.video.id = :videoId AND uv.user = :user
      """)
  int updatePositionAndLastOpened(
      @Param("videoId") UUID videoId,
      @Param("user") AppUser user,
      @Param("position") int positionSeconds
  );
}