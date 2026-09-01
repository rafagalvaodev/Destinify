package com.maisprati.destinify.backend.domain.dto.LoginDTO;

import jakarta.validation.constraints.NotBlank;

public record LoginData(
        @NotBlank String email,
        @NotBlank String password) {
}
