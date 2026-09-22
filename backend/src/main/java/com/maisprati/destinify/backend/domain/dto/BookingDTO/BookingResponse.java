package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long userId,
        String userName,
        Long hotelId,
        String hotelName,
        List<RoomResponse> rooms,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestsCount,
        Float totalPrice,
        BookingStatus status,
        LocalDateTime createdAt

) {
}
