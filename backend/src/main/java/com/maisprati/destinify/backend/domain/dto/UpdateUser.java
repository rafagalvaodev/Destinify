package com.maisprati.destinify.backend.domain.dto;

public record UpdateUser(
        String name,
        String email,
        String password) {
}
