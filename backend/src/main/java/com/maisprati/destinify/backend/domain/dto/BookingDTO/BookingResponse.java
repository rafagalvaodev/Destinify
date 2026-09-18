package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import com.maisprati.destinify.backend.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        String userName,
        Long hotelId,
        String hotelName,
        Long roomId,
        String roomName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestsCount,
        Float totalPrice,
        BookingStatus status,
        LocalDateTime createdAt

) {
}
