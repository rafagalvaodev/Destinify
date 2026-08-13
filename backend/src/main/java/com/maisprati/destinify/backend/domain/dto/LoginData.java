package com.maisprati.destinify.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginData(
        @NotBlank String email,
        @NotBlank String password) {
}
