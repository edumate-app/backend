package com.example.auth.expression.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record VerbConjugationForm(
    @Column(name = "person", nullable = false) String person,
    @Column(name = "form_value", nullable = false) String form
) {
}
