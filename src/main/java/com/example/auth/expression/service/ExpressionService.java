package com.example.auth.expression.service;

import com.example.auth.expression.dto.*;
import com.example.auth.expression.entity.Expression;
import com.example.auth.expression.entity.ExpressionContext;
import com.example.auth.expression.repository.ExpressionContextRepository;
import com.example.auth.expression.repository.ExpressionRepository;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.AnalyzeRequest;
import com.example.auth.nlp.entity.LemmaConjugation;
import com.example.auth.nlp.repository.LemmaConjugationRepository;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.Video;
import com.example.auth.video.exception.ExpressionNotFoundException;
import com.example.auth.video.exception.TranscriptSegmentNotFoundException;
import com.example.auth.video.exception.VideoNotFoundException;
import com.example.auth.video.repository.VideoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExpressionService {
  private final NlpClient nlpClient;
  private final ExpressionRepository expressionRepository;
  private final VideoRepository videoRepository;
  private final ExpressionContextRepository expressionContextRepository;
  private final LemmaConjugationRepository lemmaConjugationRepository;

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
    Integer startSeconds = segment.getStart().intValue();
    String lang = video.getTargetLang();

    var byLemma = request.expressions().stream()
        .collect(Collectors.groupingBy(
            SaveExpressionDto::lemma,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    byLemma.forEach((lemma, dtos) -> {
      SaveExpressionDto first = dtos.getFirst();

      upsertLemmaConjugation(lang, lemma, first.pos(), first.conjugation());

      Expression expression = expressionRepository.findByUserAndLangAndLemma(user, lang, lemma)
          .orElseGet(() -> expressionRepository.save(
              Expression.builder()
                  .user(user)
                  .lang(lang)
                  .lemma(lemma)
                  .lemmaTranslation(first.lemmaTranslation())
                  .pos(first.pos())
                  .build()
          ));

      List<String> newForms = dtos.stream()
          .map(SaveExpressionDto::text)
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      ExpressionContext context = expressionContextRepository
          .findByExpressionAndTranscriptSegment(expression, segment)
          .orElse(null);

      if (context != null) {
        var merged = new LinkedHashSet<>(context.getMatchedForms());
        merged.addAll(newForms);
        context.setMatchedForms(new ArrayList<>(merged));
      } else {
        context = ExpressionContext.builder()
            .expression(expression)
            .transcriptSegment(segment)
            .targetSentence(segment.getTargetText())
            .nativeTranslation(segment.getNativeText())
            .matchedForms(new ArrayList<>(newForms))
            .video(video)
            .startSeconds(startSeconds)
            .build();
      }

      expressionContextRepository.save(context);
    });
  }

  @Transactional(readOnly = true)
  public List<ExpressionDto> getUserExpressions(AppUser user) {
    List<Expression> expressions = expressionRepository.findAllByUser(user);
    if (expressions.isEmpty()) {
      return List.of();
    }

    Set<String> langs = expressions.stream()
        .map(Expression::getLang)
        .collect(Collectors.toSet());
    Set<String> lemmas = expressions.stream()
        .map(Expression::getLemma)
        .collect(Collectors.toSet());

    Map<String, List<VerbConjugationForm>> conjugationsByKey = lemmaConjugationRepository
        .findByLangInAndLemmaIn(langs, lemmas)
        .stream()
        .collect(Collectors.toMap(
            lc -> conjugationKey(lc.getLang(), lc.getLemma()),
            LemmaConjugation::getForms,
            (a, b) -> a
        ));

    return expressions.stream()
        .map(ex -> new ExpressionDto(
            ex.getId(),
            ex.getLemma(),
            ex.getLemmaTranslation(),
            ex.getPos(),
            conjugationsByKey.getOrDefault(
                conjugationKey(ex.getLang(), ex.getLemma()),
                List.of()
            ),
            ex.getAddedAt()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ContextDto> getContexts(AppUser user, UUID expressionId) {
    Expression expression = expressionRepository.findById(expressionId)
        .filter(e -> e.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ExpressionNotFoundException(expressionId));

    return expressionContextRepository.findAllByExpressionIdWithVideo(expression.getId()).stream()
        .map(ec -> new ContextDto(
            ec.getTargetSentence(),
            ec.getNativeTranslation(),
            ec.getVideo().getId(),
            ec.getVideo().getTitle(),
            ec.getMatchedForms(),
            ec.getStartSeconds()
        ))
        .toList();
  }

  private void upsertLemmaConjugation(
      String lang,
      String lemma,
      PosType pos,
      List<VerbConjugationForm> forms
  ) {
    if (pos != PosType.VERB && pos != PosType.AUX) {
      return;
    }
    if (forms == null || forms.isEmpty()) {
      return;
    }

    LemmaConjugation conjugation = lemmaConjugationRepository
        .findByLangAndLemma(lang, lemma)
        .orElseGet(() -> LemmaConjugation.builder()
            .lang(lang)
            .lemma(lemma)
            .forms(new ArrayList<>())
            .build());

    if (conjugation.getForms() == null || conjugation.getForms().isEmpty()) {
      conjugation.setForms(new ArrayList<>(forms));
      lemmaConjugationRepository.save(conjugation);
    }
  }

  private static String conjugationKey(String lang, String lemma) {
    return lang + '\0' + lemma;
  }
}
