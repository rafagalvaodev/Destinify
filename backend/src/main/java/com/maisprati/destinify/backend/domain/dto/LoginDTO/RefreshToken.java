package com.maisprati.destinify.backend.domain.dto.LoginDTO;

import jakarta.validation.constraints.NotBlank;

public record RefreshToken(@NotBlank String refreshToken) {
}
