package com.example.auth.expression.dto;

import java.util.List;

public record SaveExpressionDto(
    String text,
    String lemma,
    String translation,
    PosType pos,
    List<VerbConjugationForm> conjugation
) {
}
