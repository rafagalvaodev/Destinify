package com.maisprati.destinify.backend.domain.dto.UserDTO;

public record UpdateUser(
        String name,
        String email,
        String password) {
}
