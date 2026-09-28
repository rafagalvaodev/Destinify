package com.maisprati.destinify.backend.domain.dto.UserDTO;

import java.time.LocalDate;

public record UpdateUser(
        String name,
        String email,
        String password,
        LocalDate birthDate) {
}
