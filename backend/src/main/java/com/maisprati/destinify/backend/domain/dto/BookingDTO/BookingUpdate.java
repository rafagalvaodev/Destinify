package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import java.time.LocalDate;

public record BookingUpdate(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestsCount
) {
}
