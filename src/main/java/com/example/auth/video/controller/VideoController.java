package com.example.auth.video.controller;

import com.example.auth.user.entity.AppUser;
import com.example.auth.video.dto.LanguageDto;
import com.example.auth.video.dto.TranscriptSegmentDto;
import com.example.auth.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
  private final VideoService videoService;
  @PostMapping("/validation")
  public List<LanguageDto> validation(@RequestParam String url) {
    return videoService.getAvailableLang(url);
  }

  @PostMapping("/add")
  public void addVideo(@RequestParam String url,
                       @AuthenticationPrincipal AppUser user) {
    videoService.addVideo(url, user);
  }

  @GetMapping("/transcript")
  public List<TranscriptSegmentDto> getTranscript(@RequestParam String video_id) {
    return videoService.getTranscript(video_id);
  }
}
