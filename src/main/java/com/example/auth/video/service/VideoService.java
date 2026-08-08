package com.example.auth.video.service;

import com.example.auth.job.JobNotFoundException;
import com.example.auth.job.RedisJobStore;
import com.example.auth.job.dto.JobStatusDto;
import com.example.auth.job.record.JobRecord;
import com.example.auth.job.record.JobStatus;
import com.example.auth.job.record.JobType;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.video.dto.*;
import com.example.auth.video.exception.InvalidVideoUrlException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.Video;
import com.example.auth.video.repository.UserVideoRepository;
import com.example.auth.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VideoService {
  private final NlpClient nlpClient;

  private final UserVideoRepository userVideoRepository;
  private final VideoRepository videoRepository;
  private final RedisJobStore redisJobStore;
  private final VideoImportRunner videoImportRunner;
  private static final Logger log = LoggerFactory.getLogger(VideoService.class);

  @Transactional
  public void removeVideo(UUID videoId, AppUser user) {
    int deleted = userVideoRepository.deleteByUserAndVideo_Id(videoId, user);
    if (deleted == 0) {
      throw new VideoNotFoundException(videoId);
    }
  }

  public List<LanguageDto> getAvailableLang(String url, AppUser user) {
    String videoId = extractVideoId(url);

    List<NlpLanguageDto> languages = nlpClient.getAvailableLang(videoId);

    Set<String> importedLangs = new HashSet<>(
        userVideoRepository.findTargetLangsByVideoIdAndUser(videoId, user)
    );

    return languages.stream()
        .map(lang -> new LanguageDto(
            lang.language(),
            lang.language_code(),
            importedLangs.contains(lang.language_code())
        ))
        .toList();
  }

  @Transactional
  public ImportResponse importVideo(String url, String targetLang, AppUser user) {
    String videoId = extractVideoId(url);

    String jobId = videoId + ":" + targetLang.toLowerCase(Locale.ROOT);

    Optional<JobRecord> existing = redisJobStore.find(jobId);

    if (existing.isPresent()) {
      JobStatus status = existing.get().status();
      if (status == JobStatus.PENDING || status == JobStatus.RUNNING) {
        redisJobStore.addUser(jobId, user.getId());
        // Late join: Video may already exist after SAVE_VIDEO — link immediately.
        videoRepository.findByVideoIdAndTargetLang(videoId, targetLang)
            .ifPresent(video -> videoImportRunner.ensureUserVideo(user, video));
        return new ImportResponse(jobId);
      }
    }

    JobRecord job = JobRecord.create(jobId, JobType.VIDEO_IMPORT);
    redisJobStore.save(job, user.getId());

    videoImportRunner.processAsync(
        jobId,
        videoId,
        targetLang,
        user.getId(),
        user.getNativeLang()
    );

    return new ImportResponse(jobId);
  }

  public List<JobStatusDto> listImportJobs(AppUser user) {
    return redisJobStore.findAllByUser(user.getId()).stream()
        .filter(job -> job.type() == JobType.VIDEO_IMPORT)
        .map(JobStatusDto::fromVideo)
        .toList();
  }

  public JobRecord requireOwnedJob(String jobId, AppUser user) {
    return redisJobStore.find(jobId, user.getId())
        .orElseThrow(() -> new JobNotFoundException(jobId));
  }

  public List<VideoDto> getVideos(AppUser user) {
    return userVideoRepository.findTop10ByUserOrderByLastOpenedAtDesc(user)
        .stream()
        .map(uv -> {
          Video video = uv.getVideo();
          return new VideoDto(
              video.getId(),
              video.getTargetLang(),
              video.getVideoId(),
              video.getTitle(),
              video.getAuthor(),
              video.getDuration(),
              uv.getLastOpenedAt(),
              uv.getLastPositionSeconds()
          );
        })
        .toList();
  }

  @Transactional
  public void updatePosition(UUID videoId, AppUser user, int positionSeconds) {
    log.info("Updating position for video: {} to {}", videoId, positionSeconds);

    int updatedRows = userVideoRepository.updatePositionAndLastOpened(videoId, user, positionSeconds);

    if (updatedRows == 0) {
      throw new VideoNotFoundException(videoId);
    }
  }

  private String extractVideoId(String url) {
    Pattern pattern = Pattern.compile(
        "(?:youtu\\.be/|youtube\\.com(?:/watch\\?v=|/embed/|/shorts/))([^?&/]+)"
    );

    Matcher matcher = pattern.matcher(url);

    if (!matcher.find()) {
      throw new InvalidVideoUrlException();
    }

    String videoId = matcher.group(1);

    if (videoId.length() != 11) {
      throw new InvalidVideoUrlException();
    }

    return matcher.group(1);
  }
}
