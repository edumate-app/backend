package com.example.auth.nlp.service;

import com.example.auth.expression.dto.PosType;
import com.example.auth.expression.dto.VerbConjugationForm;
import com.example.auth.nlp.entity.LemmaConjugation;
import com.example.auth.nlp.repository.LemmaConjugationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LemmaConjugationService {
  private final LemmaConjugationRepository lemmaConjugationRepository;

  @Transactional
  public void upsert(String lang, String lemma, PosType pos, List<VerbConjugationForm> forms) {
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

  public Map<String, List<VerbConjugationForm>> getConjugationsByLemma(String lang, Set<String> lemmas) {
    return lemmaConjugationRepository
        .findByLangInAndLemmaIn(Set.of(lang), lemmas)
        .stream()
        .collect(Collectors.toMap(
            LemmaConjugation::getLemma,
            LemmaConjugation::getForms,
            (a, b) -> a
        ));
  }

  public Map<String, List<VerbConjugationForm>> getConjugationsByLangAndLemma(
      Set<String> langs, Set<String> lemmas) {
    return lemmaConjugationRepository
        .findByLangInAndLemmaIn(langs, lemmas)
        .stream()
        .collect(Collectors.toMap(
            lc -> conjugationKey(lc.getLang(), lc.getLemma()),
            LemmaConjugation::getForms,
            (a, b) -> a
        ));
  }

  public static String conjugationKey(String lang, String lemma) {
    return lang + '\0' + lemma;
  }
}
