package com.example.auth.expression.dto;

import java.util.List;
import java.util.UUID;

public record ExpressionDto (
    UUID id,
    String text,
    String lemmaTranslation,
    PosType pos,
    List<VerbConjugationForm> conjugation
) {
}
