package com.maisprati.destinify.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UserCreate(
        @NotBlank String name,
        @NotBlank String email,
        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull LocalDate birthdate,
        @NotBlank String password) {
}
