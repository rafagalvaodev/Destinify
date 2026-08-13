package com.maisprati.destinify.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreate(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String password) {
}
