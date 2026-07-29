package com.example.auth.expression.dto;

import java.util.List;

public record ExpressionDto (
    String text,
    String translation,
    PosType pos,
    List<VerbConjugationForm> conjugation
) {
}
