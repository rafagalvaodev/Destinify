package com.maisprati.destinify.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshToken(@NotBlank String refreshToken) {
}
