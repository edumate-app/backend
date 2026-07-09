package com.example.auth.video.dto;

import java.time.Instant;
import java.util.UUID;

public record VideoDto(
    UUID uuid,
    String targetLang,
    String videoId,
    String title,
    String author,
    int duration,
    Instant lastOpenedAt,
    int lastPositionSeconds
) {}
