package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus paymentStatus);

    Optional<Payment> findByIdAndBookingId(Long paymentId, Long bookingId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);


}
