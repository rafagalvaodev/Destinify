package com.maisprati.destinify.backend.exceptions;

public class RoomNotFoundException extends ResourceNotFoundException {
    public RoomNotFoundException(Long id) {
        super(String.format("Room with id %d not found", id));
    }
}
