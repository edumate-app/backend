package com.example.auth.expression.dto;

import java.util.List;
import java.util.UUID;

public record SaveExpressionRequest(
    List<SaveExpressionDto> expressions,
    int contextIndex,
    UUID video_uuid
) {
}
