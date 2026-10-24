package com.maisprati.destinify.backend.exceptions;

public class BookingCancellationDeadlineException extends RuntimeException {
    public BookingCancellationDeadlineException() {
        super("O cancelamento exige mais de 72 horas de antecedência");
    }
}
