package com.example.auth.video.service;

import com.example.auth.expression.dto.WordAnalyzedDto;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.NlpAnalyzeRequest;
import com.example.auth.nlp.dto.InstallStatus;
import com.example.auth.nlp.dto.InstallStatusResponse;
import com.example.auth.nlp.service.LemmaConjugationService;
import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.TranscriptToken;
import com.example.auth.video.entity.Video;
import com.example.auth.video.repository.TranscriptSegmentRepository;
import com.example.auth.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TranscriptAnalysisService {
  private static final Logger log = LoggerFactory.getLogger(TranscriptAnalysisService.class);
  private static final Duration INSTALL_POLL_INTERVAL = Duration.ofSeconds(5);
  private static final Duration INSTALL_TIMEOUT = Duration.ofMinutes(5);

  private final VideoRepository videoRepository;
  private final TranscriptSegmentRepository transcriptSegmentRepository;
  private final NlpClient nlpClient;
  private final LemmaConjugationService lemmaConjugationService;
  private final PlatformTransactionManager transactionManager;

  @Async
  public void analyzeVideoAsync(UUID videoId) {
    Video video = videoRepository.findById(videoId).orElse(null);
    if (video == null) {
      log.warn("Skipping transcript analysis – video not found: {}", videoId);
      return;
    }

    String lang = video.getTargetLang();
    List<UUID> segmentIds = transcriptSegmentRepository.findIdsWithoutTokensByVideoId(videoId);
    if (segmentIds.isEmpty()) {
      log.info("Transcript analysis skipped – all segments already tokenized for video {}", videoId);
      return;
    }

    if (!waitUntilLanguageReady(lang)) {
      log.error(
          "Skipping transcript analysis for video {} – language {} not ready in time",
          videoId,
          lang
      );
      return;
    }

    log.info("Starting background transcript analysis for video {} ({} segments)", videoId, segmentIds.size());

    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    int analyzed = 0;
    int failed = 0;

    for (UUID segmentId : segmentIds) {
      try {
        boolean saved = Boolean.TRUE.equals(tx.execute(status -> analyzeSegment(segmentId, lang)));
        if (saved) {
          analyzed++;
        }
      } catch (Exception e) {
        failed++;
        log.warn("Failed to analyze segment {} of video {}: {}", segmentId, videoId, e.getMessage());
      }
    }

    log.info(
        "Finished background transcript analysis for video {}: analyzed={}, failed={}",
        videoId,
        analyzed,
        failed
    );
  }

  private boolean waitUntilLanguageReady(String lang) {
    Instant deadline = Instant.now().plus(INSTALL_TIMEOUT);
    boolean installRequested = false;

    while (Instant.now().isBefore(deadline)) {
      InstallStatusResponse statusDto = nlpClient.getInstallStatus(lang);
      InstallStatus status = statusDto != null ? statusDto.status() : null;

      if (status == InstallStatus.ready) {
        log.info("Language {} is ready for analysis", lang);
        return true;
      }

      if (status == InstallStatus.failed) {
        log.error("Language {} installation failed", lang);
        return false;
      }

      if (!installRequested && status != InstallStatus.installing) {
        log.info("Requesting installation for language {} (status={})", lang, status);
        nlpClient.installLanguage(lang);
        installRequested = true;
      } else {
        log.info("Waiting for language {} (status={})", lang, status);
      }

      try {
        Thread.sleep(INSTALL_POLL_INTERVAL.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Interrupted while waiting for language {}", lang);
        return false;
      }
    }

    log.error("Timed out waiting for language {} to become ready", lang);
    return false;
  }

  private boolean analyzeSegment(UUID segmentId, String lang) {
    TranscriptSegment segment = transcriptSegmentRepository.findByIdWithTokens(segmentId)
        .orElse(null);
    if (segment == null) {
      return false;
    }
    if (!segment.getTokens().isEmpty()) {
      return false;
    }

    String text = segment.getTargetText();
    if (text == null || text.isBlank()) {
      return false;
    }

    List<WordAnalyzedDto> words = nlpClient.getAnalysis(new NlpAnalyzeRequest(text, lang));
    if (words == null || words.isEmpty()) {
      return false;
    }

    for (int i = 0; i < words.size(); i++) {
      WordAnalyzedDto word = words.get(i);
      segment.addToken(TranscriptToken.builder()
          .tokenIndex(i)
          .text(word.text())
          .lemma(word.lemma())
          .pos(word.pos())
          .person(word.person())
          .number(word.number())
          .tense(word.tense())
          .mood(word.mood())
          .gender(word.gender())
          .build());

      lemmaConjugationService.upsert(lang, word.lemma(), word.pos(), word.conjugation());
    }

    transcriptSegmentRepository.save(segment);
    return true;
  }
}
