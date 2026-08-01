package com.example.auth.nlp.entity;

import com.example.auth.expression.dto.VerbConjugationForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "lemma_conjugation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_lemma_conjugation_lang_lemma",
        columnNames = {"lang", "lemma"}
    )
)
public class LemmaConjugation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 16)
  private String lang;

  @Column(nullable = false)
  private String lemma;

  @ElementCollection
  @CollectionTable(
      name = "lemma_conjugation_forms",
      joinColumns = @JoinColumn(name = "lemma_conjugation_id")
  )
  @OrderColumn(name = "sort_order")
  @Builder.Default
  private List<VerbConjugationForm> forms = new ArrayList<>();
}
