package com.example.auth.video.dto;

public record TranscriptSegmentDto (String nativeText, String targetText, Double start, Double duration) {
}
