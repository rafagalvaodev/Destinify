package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import java.time.LocalDate;

public record BookingUpdate(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(60) Integer guestsCount,
        @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(60) Integer adultsCount,
        @jakarta.validation.constraints.Size(max = 59)
        java.util.List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(17) Integer> childrenAges
) {
}
