package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Mapper de Payment

@DisplayName("PaymentMapper")
class PaymentMapperTest {

    @Test
    @DisplayName("paymentResponseMapper converte o pagamento e usa só o id da reserva")
    void paymentResponseMapper_copiaOsCampos() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(42L);
        LocalDateTime criado = LocalDateTime.of(2030, 1, 10, 9, 0);
        LocalDateTime processado = LocalDateTime.of(2030, 1, 10, 9, 5);
        Payment payment = new Payment(8L, booking, new BigDecimal("300.00"),
                PaymentStatus.APPROVED, criado, processado);

        // Act
        PaymentResponse response = new PaymentMapper().paymentResponseMapper(payment);

        // Assert
        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.bookingId()).isEqualTo(42L);
        assertThat(response.amount()).isEqualByComparingTo("300.00");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.createdAt()).isEqualTo(criado);
        assertThat(response.processedAt()).isEqualTo(processado);
    }
}
