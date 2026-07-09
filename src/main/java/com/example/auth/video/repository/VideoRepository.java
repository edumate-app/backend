package com.example.auth.video.repository;

import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

  List<Video> findAllByUser(AppUser user);
}
