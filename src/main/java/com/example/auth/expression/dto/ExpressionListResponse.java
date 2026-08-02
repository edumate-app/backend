package com.example.auth.expression.dto;

import java.util.List;

public record ExpressionListResponse(
    List<ExpressionDto> expressions,
    List<String> languages
) {
}
