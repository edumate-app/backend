package com.example.auth.expression.dto;

import java.util.List;
import java.util.UUID;

public record ContextDto(
    String targetSentence,
    String nativeTranslation,
    UUID video_uuid,
    String videoTitle,
    List<String> matchedForms,
    Integer startSeconds
) {
}
