package com.maisprati.destinify.backend.exceptions;

public class BookingNotFoundException extends ResourceNotFoundException {
    public BookingNotFoundException(Long id) {
        super(String.format("Booking with id %d not found", id));
    }

    public BookingNotFoundException(String text) {
        super(text);
    }
}
