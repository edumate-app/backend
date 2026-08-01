package com.example.auth.nlp.dto;

public record NlpTranscriptSegmentDto(String nativeText, String targetText, Double start, Double duration) {
}
