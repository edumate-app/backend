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
}
