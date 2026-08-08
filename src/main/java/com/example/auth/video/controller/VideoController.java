package com.example.auth.video.controller;

import com.example.auth.job.JobSseService;
import com.example.auth.job.dto.JobStatusDto;
import com.example.auth.job.record.JobRecord;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.dto.*;
import com.example.auth.video.service.TranscriptService;
import com.example.auth.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
  private final VideoService videoService;
  private final TranscriptService transcriptService;
  private final JobSseService jobSseService;
  @PostMapping("/validation")
  public List<LanguageDto> validation(@RequestParam String url,
                                      @AuthenticationPrincipal AppUser user) {
    return videoService.getAvailableLang(url, user);
  }

  @GetMapping("/import/jobs")
  public List<JobStatusDto> listImportJobs(@AuthenticationPrincipal AppUser user) {
    return videoService.listImportJobs(user);
  }

  @PostMapping("/import")
  public ImportResponse addVideo(@RequestBody ImportRequest request,
                                 @AuthenticationPrincipal AppUser user) {
    return videoService.importVideo(request.url(), request.targetLang() , user);
  }

  @GetMapping(value = "/import/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter importEvents(@PathVariable String jobId,
                                 @AuthenticationPrincipal AppUser user) {
    JobRecord job = videoService.requireOwnedJob(jobId, user);
    return jobSseService.subscribe(JobStatusDto.fromVideo(job));
  }

  @GetMapping("/transcript/{videoUUID}")
  public TranscriptResponseDto getTranscript(@PathVariable UUID videoUUID,
                                             @AuthenticationPrincipal AppUser user) {
    return transcriptService.getTranscript(videoUUID, user);
  }

  @DeleteMapping("/{videoUUID}")
  public ResponseEntity<Void> removeVideo(@PathVariable UUID videoUUID,
                          @AuthenticationPrincipal AppUser user) {
    videoService.removeVideo(videoUUID, user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public List<VideoDto> getVideos(@AuthenticationPrincipal AppUser user) {
    return videoService.getVideos(user);
  }

  @PatchMapping("/{videoId}/position")
  public ResponseEntity<Void> updatePosition(
      @PathVariable UUID videoId,
      @RequestBody UpdatePositionRequest request,
      @AuthenticationPrincipal AppUser user
  ) {
    videoService.updatePosition(videoId, user, request.positionSeconds());
    return ResponseEntity.ok().build();
  }
}
