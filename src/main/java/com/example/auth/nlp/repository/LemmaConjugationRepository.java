package com.example.auth.nlp.repository;

import com.example.auth.nlp.entity.LemmaConjugation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemmaConjugationRepository extends JpaRepository<LemmaConjugation, UUID> {
  Optional<LemmaConjugation> findByLangAndLemma(String lang, String lemma);

  List<LemmaConjugation> findByLangInAndLemmaIn(Collection<String> langs, Collection<String> lemmas);
}
