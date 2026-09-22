package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.servicies.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

//    @PostMapping("/{bookingId}/payment")
//    public ResponseEntity<PaymentResponse> create(
//            @PathVariable Long bookingId,
//            @AuthenticationPrincipal User loggedUser) {
//        return ResponseEntity.ok(paymentService.createPayment(bookingId, loggedUser));
//    }
//
//    @PostMapping("/{bookingId}/payment/{paymentId}/approve")
//    public ResponseEntity<PaymentResponse> approve(
//            @PathVariable Long bookingId,
//            @PathVariable Long paymentId,
//            @AuthenticationPrincipal User loggedUser) {
//        return ResponseEntity.ok(paymentService.processPayment(
//                bookingId,
//                paymentId,
//                loggedUser,
//                PaymentStatus.APPROVED));
//    }
//
//    @PostMapping("/{bookingId}/payment/{paymentId}/reject")
//    public ResponseEntity<PaymentResponse> reject(
//            @PathVariable Long bookingId,
//            @PathVariable Long paymentId,
//            @AuthenticationPrincipal User loggedUser) {
//        return ResponseEntity.ok(paymentService.processPayment(
//                bookingId,
//                paymentId,
//                loggedUser,
//                PaymentStatus.REJECTED));
//    }

    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User loggedUser) {

        PaymentResponse pay = paymentService.pay(bookingId, loggedUser);

        return ResponseEntity.ok(pay);
    }
}
