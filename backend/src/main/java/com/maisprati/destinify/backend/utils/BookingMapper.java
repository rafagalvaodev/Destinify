package com.maisprati.destinify.backend.utils;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;

import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse bookingResponseMapper(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                booking.getHotel().getHotel_id(),
                booking.getHotel().getName(),
                booking.getRoom().getRoom_id(),
                booking.getRoom().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuestsCount(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
