package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;

import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    @Autowired
    private RoomMapper roomMapper;

    public BookingResponse bookingResponseMapper(Booking booking) {

        List<RoomResponse> rooms =
                booking
                        .getRooms()
                        .stream()
                        .map(roomMapper::roomResponseMapper)
                        .toList();


        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                booking.getHotel().getHotel_id(),
                booking.getHotel().getName(),
                rooms,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuestsCount(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
