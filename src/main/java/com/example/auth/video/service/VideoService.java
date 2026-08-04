package com.example.auth.video.service;

import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.nlp.dto.VideoInfo;
import com.example.auth.video.dto.*;
import com.example.auth.video.entity.UserVideo;
import com.example.auth.video.exception.InvalidVideoUrlException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.Video;
import com.example.auth.video.repository.UserVideoRepository;
import com.example.auth.video.repository.VideoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class VideoService {
  private final NlpClient nlpClient;

  private final TranscriptAnalysisService transcriptAnalysisService;
  private final TranscriptService transcriptService;

  private final VideoRepository videoRepository;
  private final UserVideoRepository userVideoRepository;
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
    // boolean[] instead of boolean to avoid "Variable used in lambda expression should be final or effectively final"
    boolean[] createdWithSegments = {false};

    Video video = videoRepository.findByVideoIdAndTargetLang(videoId, targetLang)
        .orElseGet(() -> {
          VideoInfo info = nlpClient.getVideoInfo(videoId);
          Video v = Video.builder()
              .targetLang(targetLang)
              .title(info.title())
              .author(info.author())
              .videoId(videoId)
              .duration(info.duration())
              .build();

          createdWithSegments[0] = transcriptService.attachTranscriptIfPossible(
              v, videoId, user.getNativeLang()
          );

          return videoRepository.save(v);
        });

    userVideoRepository.findByUserAndVideo_Id(user, video.getId())
        .orElseGet(() -> userVideoRepository.save(
            UserVideo.builder()
                .user(user)
                .video(video)
                .lastOpenedAt(Instant.now())
                .lastPositionSeconds(0)
                .build()
        ));

    if (createdWithSegments[0]) {
      transcriptAnalysisService.scheduleTranscriptAnalysis(video.getId());
    }

    return new ImportResponse(video.getId());
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
