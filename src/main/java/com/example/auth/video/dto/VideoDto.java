package com.example.auth.video.dto;

import java.util.UUID;

public record VideoDto(
    UUID uuid,
    String targetLang,
    String videoId
) {}
