package com.example.auth.expression.dto;

import java.util.List;

public record WordAnalyzedDto(
    String text,
    String lemma,
    PosType pos,
    NumberType number,
    Integer person,
    String tense,
    String mood,
    String gender,
    List<VerbConjugationForm> conjugation
) {
}
