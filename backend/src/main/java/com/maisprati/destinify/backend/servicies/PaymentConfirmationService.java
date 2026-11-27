package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.GatewayOutcome;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankCheckoutRequestFactory;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentConfirmationService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentConfirmationService(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public GatewayOutcome approve(Long paymentId, int paidCents) {

        Payment payment = paymentRepository.findLockedById(paymentId).orElse(null);
        if (payment == null) {
            return GatewayOutcome.NOT_FOUND;
        }
        if (payment.getPaymentStatus() == PaymentStatus.APPROVED) {
            return GatewayOutcome.ALREADY_PROCESSED;
        }
        if (PagBankCheckoutRequestFactory.emCentavos(payment.getAmount()) != paidCents) {
            return GatewayOutcome.AMOUNT_MISMATCH;
        }
        Booking booking = payment.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING) {
            return GatewayOutcome.BOOKING_NOT_PENDING;
        }

        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setProcessedAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
        paymentRepository.save(payment);
        return GatewayOutcome.APPROVED;
    }

    @Transactional
    public boolean rejectIfPending(Long paymentId) {
        Payment payment = paymentRepository.findLockedById(paymentId).orElse(null);
        if (payment == null || payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return false;
        }
        // A recusa encerra esta tentativa. A reserva continua disponível para pagar.
        payment.setPaymentStatus(PaymentStatus.REJECTED);
        payment.setProcessedAt(LocalDateTime.now());
        return true; // O JPA persiste a alteração no commit.
    }

    @Transactional
    public boolean clearExpiredCheckout(String checkoutId) {
        return paymentRepository.findByProviderCheckoutId(checkoutId)
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PENDING)
                .map(payment -> {
                    payment.setProviderCheckoutId(null);
                    payment.setCheckoutUrl(null);
                    payment.setExpiresAt(null);
                    // o link venceu. A reserva e o pagamento continua pendentes.
                    paymentRepository.save(payment);
                    return true;
                })
                .orElse(false);
    }

    public Object clearExpire() {
        return null;
    }
}