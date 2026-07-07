package com.example.auth.nlp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NlpTranscriptRequest (
    @JsonProperty("target_lang")
    String targetLang,

    @JsonProperty("native_lang")
    String nativeLang
) {}
