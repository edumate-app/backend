package com.example.auth.video.service;

import com.example.auth.job.JobSseService;
import com.example.auth.job.RedisJobStore;
import com.example.auth.job.dto.JobStatusDto;
import com.example.auth.job.record.JobRecord;
import com.example.auth.job.record.JobStep;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.VideoInfo;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.repository.UserRepository;
import com.example.auth.video.entity.UserVideo;
import com.example.auth.video.entity.Video;
import com.example.auth.video.repository.UserVideoRepository;
import com.example.auth.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoImportRunner {
  private static final Logger log = LoggerFactory.getLogger(VideoImportRunner.class);

  private final NlpClient nlpClient;

  private final RedisJobStore jobStore;

  private final JobSseService jobSseService;
  private final TranscriptService transcriptService;
  private final TranscriptAnalysisService transcriptAnalysisService;

  private final UserRepository userRepository;
  private final VideoRepository videoRepository;
  private final UserVideoRepository userVideoRepository;

  // Avoid passing the AppUser to the function
  @Async
  public void processAsync(
      UUID jobId,
      String videoId,
      String targetLang,
      UUID userId,
      String nativeLang
  ) {
    try {
      publish(jobId, JobStep.QUEUED, 5);

      AppUser user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

      publish(jobId, JobStep.FETCH_VIDEO_INFO, 15);

      Video video = videoRepository.findByVideoIdAndTargetLang(videoId, targetLang)
          .orElse(null);

      boolean shouldAnalyze = false;

      if (video == null) {
        publish(jobId, JobStep.FETCH_TRANSCRIPT, 40);
        VideoInfo info = nlpClient.getVideoInfo(videoId);
        video = Video.builder()
            .targetLang(targetLang)
            .title(info.title())
            .author(info.author())
            .videoId(videoId)
            .duration(info.duration())
            .build();

        shouldAnalyze = transcriptService.attachTranscriptIfPossible(video, nativeLang);
        videoRepository.save(video);
      } else {
        // TODO: Refine this to analyze only missing segments instead of all segments.
        shouldAnalyze = transcriptAnalysisService.hasUntokenizedSegments(video.getId());
      }

      publish(jobId, JobStep.SAVE_VIDEO, 55, video.getId());
      Video finalVideo = video;
      userVideoRepository.findByUserAndVideo_Id(user, video.getId())
          .orElseGet(() -> userVideoRepository.save(
              UserVideo.builder()
                  .user(user)
                  .video(finalVideo)
                  .lastOpenedAt(Instant.now())
                  .lastPositionSeconds(0)
                  .build()
          ));

      if (shouldAnalyze) {
        publish(jobId, JobStep.ENSURE_LANGUAGE, 65, video.getId());
        transcriptAnalysisService.analyzeVideoForJob(
            video.getId(),
            video.getTargetLang(),
            (done, total) -> {
              int progress = total == 0 ? 95 : 70 + (25 * done / total);
              publish(jobId, JobStep.TOKENIZE_SEGMENTS, Math.min(progress, 95), finalVideo.getId());
            }
        );
      }

      complete(jobId, video.getId());
    } catch (Exception e) {
      log.error("Import job {} failed: {}", jobId, e.getMessage(), e);
      fail(jobId, e.getMessage() != null ? e.getMessage() : "Import failed");
    }
  }

  private void publish(UUID jobId, JobStep step, int progress) {
    publish(jobId, step, progress, null);
  }
  private void publish(UUID jobId, JobStep step, int progress, UUID resultId) {
    JobRecord current = jobStore.find(jobId)
        .orElseThrow(() -> new IllegalStateException("Job missing: " + jobId));
    JobRecord updated = current.running(step, progress);
    if (resultId != null) {
      updated = updated.withResult(resultId);
    }
    jobStore.update(updated);
    jobSseService.publish(JobStatusDto.fromVideo(updated));
  }

  private void complete(UUID jobId, UUID videoUuid) {
    JobRecord current = jobStore.find(jobId)
        .orElseThrow(() -> new IllegalStateException("Job missing: " + jobId));
    JobRecord done = current.completed(videoUuid);
    jobStore.update(done);
    jobSseService.publish(JobStatusDto.fromVideo(done));
    jobSseService.complete(jobId);
  }

  private void fail(UUID jobId, String error) {
    jobStore.find(jobId).ifPresent(current -> {
      JobRecord failed = current.failed(error);
      jobStore.update(failed);
      jobSseService.publish(JobStatusDto.fromVideo(failed));
      jobSseService.complete(jobId);
    });
  }
}
