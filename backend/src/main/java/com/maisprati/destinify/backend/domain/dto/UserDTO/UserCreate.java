package com.maisprati.destinify.backend.domain.dto.UserDTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCreate(
        @NotBlank String name,
        @NotBlank String email,
        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull LocalDate birthdate,
        @NotBlank
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password) {
}
