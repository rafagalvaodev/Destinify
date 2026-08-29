package com.maisprati.destinify.backend.domain.dto;

import com.maisprati.destinify.backend.domain.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role) {
}
