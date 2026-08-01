package com.example.auth.nlp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstallStatusResponse(
    String language,
    InstallStatus status,
    @JsonProperty("in_memory") boolean inMemory
) {
}
