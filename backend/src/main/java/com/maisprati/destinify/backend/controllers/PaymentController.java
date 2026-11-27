package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.CheckoutResponse;
import com.maisprati.destinify.backend.servicies.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class PaymentController {

    private PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{bookingId}/payment-status")
    public PaymentService.ReturnStatus paymentStatus(
            @PathVariable Long bookingId, @AuthenticationPrincipal User loggedUser) {
        return paymentService.getReturnStatus(bookingId, loggedUser);
    }

    @PostMapping("/{bookingId}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User loggedUser) {
        CheckoutResponse response = paymentService.startCheckout(bookingId, loggedUser);

        return ResponseEntity.ok(response);
    }
}