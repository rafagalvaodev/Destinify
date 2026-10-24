package com.maisprati.destinify.backend.domain.dto.PaymentDTO;

import com.maisprati.destinify.backend.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime processedAt) {
}
