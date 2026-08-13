package com.maisprati.destinify.backend.domain.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken) {
}
