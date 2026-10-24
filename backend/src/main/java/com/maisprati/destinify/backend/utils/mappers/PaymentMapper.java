package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse paymentResponseMapper(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getCreatedAt(),
                payment.getProcessedAt());
    }
}
