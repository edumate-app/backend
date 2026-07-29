package com.example.auth.expression.service;

import com.example.auth.expression.dto.ExpressionDto;
import com.example.auth.expression.dto.SaveExpressionDto;
import com.example.auth.expression.dto.SaveExpressionRequest;
import com.example.auth.expression.dto.WordAnalyzedDto;
import com.example.auth.expression.entity.Expression;
import com.example.auth.expression.entity.ExpressionContext;
import com.example.auth.expression.repository.ExpressionContextRepository;
import com.example.auth.expression.repository.ExpressionRepository;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.AnalyzeRequest;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.Video;
import com.example.auth.video.exception.TranscriptSegmentNotFoundException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.video.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExpressionService {
  private final NlpClient nlpClient;
  private final ExpressionRepository expressionRepository;
  private final VideoRepository videoRepository;
  private final ExpressionContextRepository expressionContextRepository;

  public List<WordAnalyzedDto> getAnalysis(AnalyzeRequest request) {
    return nlpClient.getAnalysis(request);
  }

  @Transactional
  public void saveExpressions(SaveExpressionRequest request, AppUser user) {
    Video video = videoRepository.findByIdWithSegments(request.video_uuid())
        .orElseThrow(() -> new VideoNotFoundException(request.video_uuid()));

    List<TranscriptSegment> segments = video.getTranscriptSegments();
    int contextIndex = request.contextIndex();

    if (contextIndex < 0 || contextIndex >= segments.size()) {
      throw new TranscriptSegmentNotFoundException(request.video_uuid(), contextIndex);
    }

    TranscriptSegment segment = segments.get(contextIndex);

    var byLemma = request.expressions().stream()
        .collect(Collectors.groupingBy(
            SaveExpressionDto::lemma,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    byLemma.forEach((lemma, dtos) -> {
      SaveExpressionDto first = dtos.get(0);

      Expression expression = expressionRepository.findByUserAndLemma(user, lemma)
          .orElseGet(() -> expressionRepository.save(
              Expression.builder()
                  .user(user)
                  .lemma(lemma)
                  .translation(first.translation())
                  .pos(first.pos())
                  .conjugation(first.conjugation() != null
                      ? first.conjugation()
                      : List.of())
                  .build()
          ));

      List<String> matchedForms = dtos.stream()
          .map(SaveExpressionDto::text)
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(ArrayList::new));

      ExpressionContext context = ExpressionContext.builder()
          .expression(expression)
          .targetSentence(segment.getTargetText())
          .nativeTranslation(segment.getNativeText())
          .matchedForms(matchedForms)
          .video(video)
          .startSeconds(segment.getStart().intValue())
          .build();

      expressionContextRepository.save(context);
    });
  }

  @Transactional(readOnly = true)
  public List<ExpressionDto> getUserExpressions(AppUser user) {
    List<Expression> expressions = expressionRepository.findAllByUser(user);
    return expressions
        .stream()
        .map(ex -> new ExpressionDto(
            ex.getLemma(),
            ex.getTranslation(),
            ex.getPos(),
            ex.getConjugation()
        ))
        .toList();
  }
}
