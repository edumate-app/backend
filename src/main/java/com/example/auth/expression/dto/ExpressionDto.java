package com.example.auth.expression.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExpressionDto (
    UUID id,
    String lemma,
    String lemmaTranslation,
    PosType pos,
    String lang,
    List<VerbConjugationForm> conjugation,
    LocalDateTime addedAt,
    int contextCount
) {
}
