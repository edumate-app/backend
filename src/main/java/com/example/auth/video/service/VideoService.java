package com.example.auth.video.service;

import com.example.auth.common.AfterCommit;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.nlp.dto.NlpTranscriptRequest;
import com.example.auth.nlp.dto.VideoInfo;
import com.example.auth.video.dto.*;
import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.UserVideo;
import com.example.auth.video.exception.InvalidVideoUrlException;
import com.example.auth.video.exception.NativeLanguageNotSetException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class VideoService {
  private final NlpClient nlpClient;
  private final VideoRepository videoRepository;
  private final UserVideoRepository userVideoRepository;
  private final TranscriptAnalysisService transcriptAnalysisService;
  private static final Logger log = LoggerFactory.getLogger(VideoService.class);

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

          try {
            String nativeLang = user.getNativeLang();
            if (nativeLang != null) {
              NlpTranscriptRequest request = new NlpTranscriptRequest(targetLang, nativeLang);
              List<TranscriptSegment> segments = nlpClient.getTranscript(videoId, request)
                  .stream()
                  .map(segment -> TranscriptSegment.builder()
                      .nativeText(segment.nativeText())
                      .targetText(segment.targetText())
                      .start(segment.start())
                      .duration(segment.duration())
                      .build())
                  .toList();

              v.addTranscriptSegments(segments);
              createdWithSegments[0] = !segments.isEmpty();
            }
          } catch (Exception e) {
            // If the transcript could not be retrieved, continue - it will be fetched later as a fallback
            // Log the error, but do not interrupt the import process
            System.err.println("Failed to fetch transcript during import: " + e.getMessage());
          }

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
      scheduleTranscriptAnalysis(video.getId());
    }

    return new ImportResponse(video.getId());
  }

  @Transactional
  public TranscriptResponseDto getTranscript(UUID video_uuid, AppUser user) {
    log.info("Getting transcript for video: {}", video_uuid);

    // 1: Update lastOpenedAt timestamp (1 UPDATE query)
    int updated = userVideoRepository.updateLastOpenedAt(video_uuid, user);
    if (updated == 0) {
      throw new VideoNotFoundException(video_uuid);
    }

    UserVideo userVideo = userVideoRepository.findByUserAndVideo_Id(user, video_uuid)
        .orElseThrow(() -> new VideoNotFoundException(video_uuid));

    // 2: Fetch video with segments in single query (1 SELECT with JOIN FETCH to avoid N+1)
    Video video = videoRepository.findByIdWithSegments(video_uuid)
        .orElseThrow(() -> new VideoNotFoundException(video_uuid));

    // 3: Validate user's native language is set
    String nativeLang = user.getNativeLang();
    if (nativeLang == null) {
      log.error("Native language not set for user: {}", user.getEmail());
      throw new NativeLanguageNotSetException();
    }

    // 4: Check if segments exist in database
    List<TranscriptSegment> segments = video.getTranscriptSegments();

    if (segments == null || segments.isEmpty()) {
      // Step 5: No segments found - fallback to NLP service
      log.info("No transcript found in database for video: {}. Fetching from NLP service...", video_uuid);

      String videoId = video.getVideoId();
      NlpTranscriptRequest request = new NlpTranscriptRequest(video.getTargetLang(), nativeLang);

      log.debug("Calling NLP service with videoId: {}, targetLang: {}, nativeLang: {}",
          videoId, video.getTargetLang(), nativeLang);

      List<TranscriptSegmentDto> segmentsDto = nlpClient.getTranscript(videoId, request);

      log.info("Received {} segments from NLP service for video: {}", segmentsDto.size(), video_uuid);

      // 6: Save nlpSegments to DB
      List<TranscriptSegment> nlpSegments = segmentsDto
          .stream()
          .map(segment -> TranscriptSegment.builder()
              .nativeText(segment.nativeText())
              .targetText(segment.targetText())
              .start(segment.start())
              .duration(segment.duration())
              .build())
          .toList();
      video.addTranscriptSegments(nlpSegments);
      videoRepository.save(video);

      log.info("Successfully saved {} segments to database for video: {}", nlpSegments.size(), video_uuid);

      if (!nlpSegments.isEmpty()) {
        scheduleTranscriptAnalysis(video.getId());
      }

      // 7: Return transcript from NLP service
      return new TranscriptResponseDto(
          segmentsDto,
          videoId,
          userVideo.getLastPositionSeconds(),
          video.getTargetLang()
      );
    }

    // 8: Return existing segments from database
    log.info("Returning {} segments from database for video: {}", segments.size(), video_uuid);
    return new TranscriptResponseDto(
        segments.stream()
            .map(s -> new TranscriptSegmentDto(
                s.getNativeText(),
                s.getTargetText(),
                s.getStart(),
                s.getDuration()
            ))
            .toList(),
        video.getVideoId(),
        userVideo.getLastPositionSeconds(),
        video.getTargetLang()
    );
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

  private void scheduleTranscriptAnalysis(UUID videoId) {
    AfterCommit.run(() -> transcriptAnalysisService.analyzeVideoAsync(videoId));
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
