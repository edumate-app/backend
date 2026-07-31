package com.example.auth.nlp.dto;

public record AnalyzeRequest(
    String text,
    String lang
) {
}
