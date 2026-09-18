package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record BookingCreate(

        @NotNull(message = "O id o hotel deve ser preenchido")
        Long hotelId,

        @NotNull(message = "O id do quarto deve ser preenchido")
        Long roomId,

        @NotNull(message = "A data de check-in deve ser preenchida")
        @FutureOrPresent(message = "A data de check-in não pode ser no passado")
        LocalDate checkInDate,

        @NotNull(message = "A data de check-out deve ser preenchida")
        @Future(message = "A data de check-out deve ser no futuro")
        LocalDate checkOutDate,

        @NotNull(message = "A quantidade de hóspedes deve ser preenchida")
        @Positive(message = "A quantidade de hóspedes deve ser maior que zero")
        Integer guestsCount
) {
}
