package com.example.auth.video.service;

import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpTranscriptRequest;
import com.example.auth.nlp.dto.NlpTranscriptSegmentDto;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.dto.TranscriptResponseDto;
import com.example.auth.video.dto.TranscriptSegmentDto;
import com.example.auth.video.entity.*;
import com.example.auth.video.exception.NativeLanguageNotSetException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.video.repository.SegmentNativeTranslationRepository;
import com.example.auth.video.repository.TranscriptTokenRepository;
import com.example.auth.video.repository.UserVideoRepository;
import com.example.auth.video.repository.VideoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
public class TranscriptService {
  private final NlpClient nlpClient;

  private final TranscriptAnalysisService transcriptAnalysisService;

  private final SegmentNativeTranslationRepository segmentNativeTranslationRepository;
  private final VideoRepository videoRepository;
  private final UserVideoRepository userVideoRepository;
  private final TranscriptTokenRepository transcriptTokenRepository;

  private static final Logger log = LoggerFactory.getLogger(VideoService.class);

  public boolean attachTranscriptIfPossible(Video video, String youtubeVideoId, String nativeLang) {
    if (nativeLang == null) return false;
    try {
      NlpTranscriptRequest request = new NlpTranscriptRequest(video.getTargetLang(), nativeLang);
      List<TranscriptSegment> segments = nlpClient.getTranscript(youtubeVideoId, request)
          .stream()
          .map(dto -> toSegmentWithTranslation(dto, nativeLang))
          .toList();
      if (segments.isEmpty()) return false;
      video.addTranscriptSegments(segments);
      return true;
    } catch (Exception e) {
      log.warn("Failed to fetch transcript during import: {}", e.getMessage());
      return false;
    }
  }

  public List<TranscriptToken> findTokensByLemmas(UUID videoId, Set<String> lemmas) {
    return transcriptTokenRepository.findByVideoIdAndLemmaIn(videoId, lemmas);
  }

  public Video requireVideoWithSegments(UUID videoUuid) {
    return videoRepository.findByIdWithSegments(videoUuid)
        .orElseThrow(() -> new VideoNotFoundException(videoUuid));
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

    // Step 5: No segments found - fallback to NLP service
    if (segments == null || segments.isEmpty()) {
      log.info("No transcript found in database for video: {}. Fetching from NLP service...", video_uuid);

      String videoId = video.getVideoId();
      NlpTranscriptRequest request = new NlpTranscriptRequest(video.getTargetLang(), nativeLang);

      log.debug("Calling NLP service with videoId: {}, targetLang: {}, nativeLang: {}",
          videoId, video.getTargetLang(), nativeLang);

      List<NlpTranscriptSegmentDto> segmentsDto = nlpClient.getTranscript(videoId, request);

      log.info("Received {} segments from NLP service for video: {}", segmentsDto.size(), video_uuid);

      // 6: Save nlpSegments to DB
      List<TranscriptSegment> nlpSegments = segmentsDto
          .stream()
          .map(dto -> toSegmentWithTranslation(dto, nativeLang))
          .toList();

      video.addTranscriptSegments(nlpSegments);
      videoRepository.save(video);

      log.info("Successfully saved {} segments to database for video: {}", nlpSegments.size(), video_uuid);

      if (!nlpSegments.isEmpty()) {
        transcriptAnalysisService.scheduleTranscriptAnalysis(video.getId());
      }

      // 7: Return transcript from NLP service
      return toTranscriptResponse(nlpSegments,
          loadNativeTextsFromSegments(nlpSegments, nativeLang),
          videoId, userVideo.getLastPositionSeconds(), video.getTargetLang());
    }

    // 8: Return existing segments from database
    log.info("Returning {} segments from database for video: {}", segments.size(), video_uuid);

    long translationCount = segmentNativeTranslationRepository
        .countBySegment_Video_IdAndNativeLang(video.getId(), nativeLang);

    Map<UUID, String> nativeBySegmentId;
    if (translationCount == 0) {
      log.info("No native translations for lang '{}' on video: {}. Fetching from NLP...",
          nativeLang, video_uuid);

      List<NlpTranscriptSegmentDto> segmentsDto = nlpClient.getTranscript(
          video.getVideoId(),
          new NlpTranscriptRequest(video.getTargetLang(), nativeLang)
      );

      attachNativeTranslations(segments, segmentsDto, nativeLang);
      videoRepository.save(video);
      // Translations are already on the in-memory segment collections — no extra SELECT
      nativeBySegmentId = loadNativeTextsFromSegments(segments, nativeLang);
    } else {
      nativeBySegmentId = loadNativeTexts(video.getId(), nativeLang);
    }

    return toTranscriptResponse(
        segments,
        nativeBySegmentId,
        video.getVideoId(),
        userVideo.getLastPositionSeconds(),
        video.getTargetLang()
    );
  }

  private TranscriptSegment toSegmentWithTranslation(NlpTranscriptSegmentDto dto, String nativeLang) {
    TranscriptSegment segment = TranscriptSegment.builder()
        .targetText(dto.targetText())
        .start(dto.start())
        .duration(dto.duration())
        .build();
    segment.addNativeTranslation(SegmentNativeTranslation.builder()
        .nativeLang(nativeLang)
        .nativeText(dto.nativeText())
        .build());
    return segment;
  }

  private TranscriptResponseDto toTranscriptResponse(
      List<TranscriptSegment> segments,
      Map<UUID, String> nativeBySegmentId,
      String videoId,
      int lastPositionSeconds,
      String targetLang
  ) {
    return new TranscriptResponseDto(
        segments.stream()
            .map(s -> new TranscriptSegmentDto(
                s.getId(),
                nativeBySegmentId.getOrDefault(s.getId(), ""),
                s.getTargetText(),
                s.getStart(),
                s.getDuration()
            ))
            .toList(),
        videoId,
        lastPositionSeconds,
        targetLang
    );
  }

  private Map<UUID, String> loadNativeTextsFromSegments(
      List<TranscriptSegment> segments,
      String nativeLang
  ) {
    Map<UUID, String> result = new HashMap<>();
    for (TranscriptSegment s : segments) {
      s.getNativeTranslations().stream()
          .filter(t -> nativeLang.equals(t.getNativeLang()))
          .findFirst()
          .ifPresent(t -> result.put(s.getId(), t.getNativeText()));
    }
    return result;
  }

  private void attachNativeTranslations(
      List<TranscriptSegment> existing,
      List<NlpTranscriptSegmentDto> fromNlp,
      String nativeLang
  ) {
    int n = Math.min(existing.size(), fromNlp.size());
    for (int i = 0; i < n; i++) {
      existing.get(i).addNativeTranslation(SegmentNativeTranslation.builder()
          .nativeLang(nativeLang)
          .nativeText(fromNlp.get(i).nativeText())
          .build());
    }
  }


  public Map<UUID, String> loadNativeTexts(UUID videoId, String nativeLang) {
    Map<UUID, String> result = new HashMap<>();
    for (SegmentNativeTranslation t : segmentNativeTranslationRepository
        .findBySegment_Video_IdAndNativeLang(videoId, nativeLang)) {
      result.put(t.getSegment().getId(), t.getNativeText());
    }
    return result;
  }
}
