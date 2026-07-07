package com.example.auth.video.dto;

import java.util.List;

public record TranscriptResponseDto(List<TranscriptSegmentDto> segments, String video_id) {
}
