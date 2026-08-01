package com.example.auth.nlp.dto;

public record NlpAnalyzeRequest(
    String text,
    String lang
) {
}
