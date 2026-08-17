package com.maisprati.destinify.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maisprati.destinify.backend.domain.enums.Role;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        Role role) {
}
