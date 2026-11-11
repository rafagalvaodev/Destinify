package com.maisprati.destinify.backend.domain.dto.PaymentDTO;

import java.time.LocalDateTime;

public record CheckoutResponse(
        Long paymentId,
        Long bookingId,
        String checkoutUrl,
        LocalDateTime expiresAt) {
}
