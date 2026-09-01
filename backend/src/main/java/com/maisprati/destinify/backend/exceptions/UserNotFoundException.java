package com.maisprati.destinify.backend.exceptions;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long id) {
        super(String.format("User with id %d not found", id));
    }
}
