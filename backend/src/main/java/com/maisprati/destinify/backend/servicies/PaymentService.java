package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.exceptions.ResourceNotFoundException;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import com.maisprati.destinify.backend.utils.mappers.PaymentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse createPayment(Long bookingId, User loggedUser) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUser().getId().equals(loggedUser.getId())) {
            throw new ForbiddenException("This reservation does not belong to this user");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Only pending reservations can receive payment");
        }

        return paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(
                bookingId, PaymentStatus.PENDING)
                .map(paymentMapper::paymentResponseMapper)
                .orElseGet(() -> {
                        Payment payment = new Payment();
                        payment.setBooking(booking);
                        payment.setAmount(new BigDecimal(booking.getTotalPrice().toString())
                                .setScale(2, RoundingMode.HALF_UP));
                        payment.setPaymentStatus(PaymentStatus.PENDING);
                        payment.setCreatedAt(LocalDateTime.now());

                        return paymentMapper.paymentResponseMapper(paymentRepository.save(payment));
                });
    }

    @Transactional
    public PaymentResponse processPayment(
            Long bookingId,
            Long paymentId,
            User loggedUser,
            PaymentStatus result) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUser().getId().equals(loggedUser.getId())) {
            throw new ForbiddenException("This reservation does not belong to this user");
        }

        Payment payment = paymentRepository
                .findByIdAndBookingId(paymentId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found"));

        if (booking.getStatus() != BookingStatus.PENDING
                || payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BookingConflictException("Payment is not pending");
        }

        if (result != PaymentStatus.APPROVED
                && result != PaymentStatus.REJECTED) {
            throw new BookingConflictException("Invalid result");
        }

        payment.setPaymentStatus(result);
        payment.setProcessedAt(LocalDateTime.now());

        if (result == PaymentStatus.APPROVED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return paymentMapper.paymentResponseMapper(
                paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse pay(Long bookingId, User loggedUser) {
       Booking bookingPending = bookingRepository
               .findById(bookingId)
               .orElseThrow(() -> new BookingNotFoundException(bookingId));

       if (bookingPending.getStatus() != BookingStatus.PENDING) {
           throw new BookingConflictException("This booking is not pending");
       }


        Payment pengingPayment = paymentRepository
                .findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(
                        bookingId, PaymentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Payment pending not found"));


        return processPayment(
                bookingId,
                pengingPayment.getId(),
                loggedUser,
                PaymentStatus.APPROVED
        );
    }

    @Transactional
    public void updatePendingAmount(Booking booking) {
        Payment payment = paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending payment not found"));
        payment.setAmount(BigDecimal.valueOf(booking.getTotalPrice())
                .setScale(2, RoundingMode.HALF_UP));
        paymentRepository.save(payment);
    }
}
