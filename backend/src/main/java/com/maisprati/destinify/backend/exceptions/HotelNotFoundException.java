package com.maisprati.destinify.backend.exceptions;

public class HotelNotFoundException extends ResourceNotFoundException {
    public HotelNotFoundException(Long id) {
        super(String.format("Hotel with id %d not found", id));
    }
}
