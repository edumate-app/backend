package com.example.auth.expression.dto;

public record WordAnalyzedDto(
    String text,
    String lemma,
    PosType pos,
    NumberType number,
    String tense,
    String mood,
    String gender
) {
}
