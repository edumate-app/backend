package com.example.auth.video.service;

import com.example.auth.job.JobNotFoundException;
import com.example.auth.job.RedisJobStore;
import com.example.auth.job.record.JobRecord;
import com.example.auth.job.record.JobType;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.video.dto.*;
import com.example.auth.video.exception.InvalidVideoUrlException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.Video;
import com.example.auth.video.repository.UserVideoRepository;
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

    UUID jobId = UUID.randomUUID();
    JobRecord job = JobRecord.create(jobId, JobType.VIDEO_IMPORT, user.getId().toString());
    redisJobStore.save(job);

    videoImportRunner.processAsync(
        jobId,
        videoId,
        targetLang,
        user.getId(),
        user.getNativeLang()
    );

    return new ImportResponse(jobId);
  }

  public JobRecord requireOwnedJob(UUID jobId, AppUser user) {
    JobRecord job = redisJobStore.find(jobId)
        .orElseThrow(() -> new JobNotFoundException(jobId));
    if (!job.userId().equals(user.getId().toString())) {
      throw new JobNotFoundException(jobId);
    }
    return job;
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
