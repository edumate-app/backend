package com.example.auth.video.controller;

import com.example.auth.user.entity.AppUser;
import com.example.auth.video.dto.*;
import com.example.auth.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
  private final VideoService videoService;
  @PostMapping("/validation")
  public List<LanguageDto> validation(@RequestParam String url,
                                      @AuthenticationPrincipal AppUser user) {
    return videoService.getAvailableLang(url, user);
  }

  @PostMapping("/import")
  public ImportResponse addVideo(@RequestBody ImportRequest request,
                                 @AuthenticationPrincipal AppUser user) {
    return videoService.importVideo(request.url(), request.targetLang() , user);
  }

  @GetMapping("/transcript/{videoUUID}")
  public TranscriptResponseDto getTranscript(@PathVariable UUID videoUUID) {
    return videoService.getTranscript(videoUUID);
  }

  @GetMapping
  public List<VideoDto> getVideos(@AuthenticationPrincipal AppUser user) {
    return videoService.getVideos(user);
  }

  @PatchMapping("/{videoId}/position")
  public ResponseEntity<Void> updatePosition(
      @PathVariable UUID videoId,
      @RequestBody UpdatePositionRequest request
  ) {
    videoService.updatePosition(videoId, request.positionSeconds());
    return ResponseEntity.ok().build();
  }
}
