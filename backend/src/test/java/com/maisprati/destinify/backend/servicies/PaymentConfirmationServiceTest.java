package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.domain.enums.GatewayOutcome;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentConfirmationService")
class PaymentConfirmationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentConfirmationService service;

    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(7L);
        booking.setStatus(BookingStatus.PENDING);

        payment = new Payment(3L, booking, new BigDecimal("300.00"), PaymentStatus.PENDING, LocalDateTime.now(), null);
    }

    @Test
    void recusaRepetidaPreservaAReserva() {
        when(paymentRepository.findLockedById(3L)).thenReturn(Optional.of(payment));
        assertThat(service.rejectIfPending(3L)).isTrue();
        assertThat(service.rejectIfPending(3L)).isFalse();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("tudo certo: aprova o pagamento, define processedAt e confirma a reserva")
    void approve_valorCerto_aprovaEConfirma() {
        when(paymentRepository.findLockedById(3L)).thenReturn(Optional.of(payment));

        GatewayOutcome result = service.approve(3L, 30000);

        assertThat(result).isEqualTo(GatewayOutcome.APPROVED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getProcessedAt()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("pagamento inexistente: NOT_FOUND")
    void approve_semPagamento_notFound() {
        when(paymentRepository.findLockedById(3L)).thenReturn(Optional.empty());

        assertThat(service.approve(3L, 30000)).isEqualTo(GatewayOutcome.NOT_FOUND);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("IDEMPOTÊNCIA: notificação repetida (já aprovado) não faz nada")
    void approve_jaAprovado_naoFazNada() {
        when(paymentRepository.findLockedById(3L)).thenReturn(Optional.of(payment));

        // pagou R$ 100,00 para um pagamento de R$ 300,00
        assertThat(service.approve(3L, 10000)).isEqualTo(GatewayOutcome.AMOUNT_MISMATCH);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("reserva cancelada: não ressucita, devolve BOOKING_NOT_PENDING")
    void approve_reservaCancelada_naoConfirma() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(paymentRepository.findLockedById(3L)).thenReturn(Optional.of(payment));

        assertThat(service.approve(3L, 30000)).isEqualTo(GatewayOutcome.BOOKING_NOT_PENDING);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("clearExpiredCheckout limpa id, link e validade de um pagamento PENDING")
    void clearExpiredCheckout_pendente_limpa() {
        payment.setProviderCheckoutId("CHEC_1");
        payment.setCheckoutUrl("https://pagamento.pagbank.com.br/x");
        payment.setExpiresAt(LocalDateTime.now());
        when(paymentRepository.findByProviderCheckoutId("CHEC_1")).thenReturn(Optional.of(payment));

        assertThat(service.clearExpiredCheckout("CHEC_1")).isTrue();

        assertThat(payment.getProviderCheckoutId()).isNull();
        assertThat(payment.getCheckoutUrl()).isNull();
        assertThat(payment.getExpiresAt()).isNull();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING); // continua pendente
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("clearExpiredCheckout não mexe em pagamento que já foi aprovado")
    void clearExpiredCheckout_jaAprovado_naoMexe() {
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setProviderCheckoutId("CHEC_1");
        when(paymentRepository.findByProviderCheckoutId("CHEC_1")).thenReturn(Optional.of(payment));

        assertThat(service.clearExpiredCheckout("CHEC_1")).isFalse();

        assertThat(payment.getProviderCheckoutId()).isEqualTo("CHEC_1");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("clearExpiredCheckout com id desconhecido: false")
    void clearExpiredCheckout_desconhecido_false() {
        when(paymentRepository.findByProviderCheckoutId("CHEC_X")).thenReturn(Optional.empty());

        assertThat(service.clearExpiredCheckout("CHEC_X")).isFalse();
    }
}