package com.maisprati.destinify.backend.domain.dto.UserDTO;

import jakarta.validation.constraints.NotBlank;

public record UpdatePassword(
        @NotBlank String currentPassword,
        @NotBlank String newPassword,
        @NotBlank String confirmNewPassword) {
}
