package com.example.auth.expression.service;

import com.example.auth.expression.dto.*;
import com.example.auth.expression.entity.Expression;
import com.example.auth.expression.entity.ExpressionContext;
import com.example.auth.expression.exception.ContextNotFoundException;
import com.example.auth.expression.repository.ExpressionContextRepository;
import com.example.auth.expression.repository.ExpressionRepository;
import com.example.auth.nlp.service.LemmaConjugationService;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.entity.TranscriptSegment;
import com.example.auth.video.entity.TranscriptToken;
import com.example.auth.video.entity.Video;
import com.example.auth.video.exception.ExpressionNotFoundException;
import com.example.auth.video.exception.NativeLanguageNotSetException;
import com.example.auth.video.exception.TranscriptSegmentNotFoundException;
import com.example.auth.video.service.TranscriptService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExpressionService {
  private final LemmaConjugationService lemmaConjugationService;
  private final TranscriptService transcriptService;

  private final ExpressionRepository expressionRepository;
  private final ExpressionContextRepository expressionContextRepository;

  @Transactional
  public void saveExpressions(SaveExpressionRequest request, AppUser user) {
    Video video = transcriptService.requireVideoWithSegments(request.video_uuid());

    List<TranscriptSegment> segments = video.getTranscriptSegments();
    int contextIndex = request.contextIndex();

    if (contextIndex < 0 || contextIndex >= segments.size()) {
      throw new TranscriptSegmentNotFoundException(request.video_uuid(), contextIndex);
    }

    TranscriptSegment originSegment = segments.get(contextIndex);

    String lang = video.getTargetLang();
    String nativeLang = user.getNativeLang();
    if (nativeLang == null) {
      throw new NativeLanguageNotSetException();
    }

    Map<UUID, String> nativeBySegmentId = transcriptService.loadNativeTexts(video.getId(), nativeLang);

    var byLemma = request.expressions().stream()
        .collect(Collectors.groupingBy(
            SaveExpressionDto::lemma,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    List<TranscriptToken> tokenHits = transcriptService.findTokensByLemmas
        (video.getId(), byLemma.keySet());

    Map<String, Map<TranscriptSegment, LinkedHashSet<String>>> formsByLemmaAndSegment =
        new HashMap<>();

    for (TranscriptToken token : tokenHits) {
      formsByLemmaAndSegment
          .computeIfAbsent(token.getLemma(), __ -> new LinkedHashMap<>())
          .computeIfAbsent(token.getSegment(), __ -> new LinkedHashSet<>())
          .add(token.getText());
    }

    byLemma.forEach((lemma, dtos) -> {
      SaveExpressionDto first = dtos.getFirst();

      lemmaConjugationService.upsert(lang, lemma, first.pos(), first.conjugation());

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

      List<String> requestForms = dtos.stream()
          .map(SaveExpressionDto::text)
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      Map<TranscriptSegment, LinkedHashSet<String>> formsBySegment =
          formsByLemmaAndSegment.getOrDefault(lemma, new LinkedHashMap<>());


      if (formsBySegment.isEmpty()) {
        formsBySegment.put(originSegment, new LinkedHashSet<>(requestForms));
      } else {
        formsBySegment
            .computeIfAbsent(originSegment, __ -> new LinkedHashSet<>())
            .addAll(requestForms);
      }

      formsBySegment.forEach((segment, forms) -> {
        ExpressionContext context = expressionContextRepository
            .findByExpressionAndTranscriptSegment(expression, segment)
            .orElse(null);

        if (context != null) {
          var merged = new LinkedHashSet<>(context.getMatchedForms());
          merged.addAll(forms);
          context.setMatchedForms(new ArrayList<>(merged));
        } else {
          context = ExpressionContext.builder()
              .expression(expression)
              .transcriptSegment(segment)
              .targetSentence(segment.getTargetText())
              .nativeTranslation(nativeBySegmentId.getOrDefault(segment.getId(), ""))
              .matchedForms(new ArrayList<>(forms))
              .video(video)
              .startSeconds(segment.getStart())
              .build();
        }

        expressionContextRepository.save(context);
      });
    });
  }

  @Transactional(readOnly = true)
  public ExpressionListResponse getUserExpressions(AppUser user) {
    List<Expression> expressions = expressionRepository.findAllByUser(user);
    if (expressions.isEmpty()) {
      return new ExpressionListResponse(List.of(), List.of());
    }

    Set<String> langs = expressions.stream()
        .map(Expression::getLang)
        .collect(Collectors.toSet());
    Set<String> lemmas = expressions.stream()
        .map(Expression::getLemma)
        .collect(Collectors.toSet());

    Map<String, List<VerbConjugationForm>> conjugationsByKey =
        lemmaConjugationService.getConjugationsByLangAndLemma(langs, lemmas);

    Set<UUID> ids = expressions.stream()
        .map(Expression::getId)
        .collect(Collectors.toSet());
    Map<UUID, Integer> contextCountByExpressionId = expressionContextRepository
        .countByExpressionIds(ids)
        .stream()
        .collect(Collectors.toMap(
            row -> (UUID) row[0],
            row -> ((Number) row[1]).intValue()
        ));

    List<String> languages = new ArrayList<>();

    String nativeLang = user.getNativeLang();
    if (nativeLang != null && !nativeLang.isBlank()) {
      languages.add(nativeLang);
    }

    for (String lang : langs) {
      if (!languages.contains(lang)) {
        languages.add(lang);
      }
    }

    return new ExpressionListResponse(
        expressions.stream()
          .map(ex -> new ExpressionDto(
              ex.getId(),
              ex.getLemma(),
              ex.getLemmaTranslation(),
              ex.getPos(),
              ex.getLang(),
              conjugationsByKey.getOrDefault(
                  LemmaConjugationService.conjugationKey(ex.getLang(), ex.getLemma()),
                  List.of()
              ),
              ex.getAddedAt(),
              contextCountByExpressionId.getOrDefault(ex.getId(), 0)
          ))
          .toList(),
        languages
    );
  }

  @Transactional(readOnly = true)
  public List<ContextDto> getContexts(AppUser user, UUID expressionId) {
    Expression expression = expressionRepository.findById(expressionId)
        .filter(e -> e.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ExpressionNotFoundException(expressionId));

    return expressionContextRepository.findAllByExpressionIdWithVideo(expression.getId()).stream()
        .map(ec -> new ContextDto(
            ec.getId(),
            ec.getTargetSentence(),
            ec.getNativeTranslation(),
            ec.getVideo().getId(),
            ec.getVideo().getTitle(),
            ec.getMatchedForms(),
            ec.getStartSeconds()
        ))
        .toList();
  }

  @Transactional
  public void deleteExpression(AppUser user, UUID expressionId) {
    Expression expression = expressionRepository.findById(expressionId)
        .filter(e -> e.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ExpressionNotFoundException(expressionId));

    List<ExpressionContext> contexts =
        expressionContextRepository.findAllByExpressionIdWithVideo(expressionId);
    expressionContextRepository.deleteAll(contexts);
    expressionRepository.delete(expression);
  }

  @Transactional
  public void deleteContext(AppUser user, UUID expressionId, UUID contextId) {
    Expression expression = expressionRepository.findById(expressionId)
        .filter(e -> e.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ExpressionNotFoundException(expressionId));

    ExpressionContext context = expressionContextRepository.findById(contextId)
        .filter(c -> c.getExpression().getId().equals(expression.getId()))
        .orElseThrow(() -> new ContextNotFoundException(contextId));

    expressionContextRepository.delete(context);
  }
}
