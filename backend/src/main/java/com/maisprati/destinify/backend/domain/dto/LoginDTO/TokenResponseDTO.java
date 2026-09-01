package com.maisprati.destinify.backend.domain.dto.LoginDTO;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken) {
}
