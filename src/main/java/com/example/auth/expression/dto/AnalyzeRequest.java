package com.example.auth.expression.dto;

import java.util.UUID;

public record AnalyzeRequest (
    UUID transcriptSegmentUUID,
    String text,
    String lang
) {
}
