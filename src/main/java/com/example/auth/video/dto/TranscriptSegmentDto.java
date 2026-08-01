package com.example.auth.video.dto;

import java.util.UUID;

public record TranscriptSegmentDto (UUID id, String nativeText, String targetText, Double start, Double duration) {
}
