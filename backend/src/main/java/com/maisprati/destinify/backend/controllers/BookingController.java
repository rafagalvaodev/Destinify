package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingUpdate;
import com.maisprati.destinify.backend.servicies.BookingService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreate bookingCreate,
            @AuthenticationPrincipal User loggedUser) {

        BookingResponse response = bookingService.newBooking(bookingCreate, loggedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(bookingService.findBookingById(id, loggedUser));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<BookingResponse>> findMyBookings(
            @AuthenticationPrincipal User loggedUser,
            Pageable pageable) {

        return ResponseEntity.ok(bookingService.findByUserId(loggedUser.getId(), pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<BookingResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(bookingService.findAll(pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @AuthenticationPrincipal User loggedUser,
            @PathVariable Long id,
            @Valid @RequestBody BookingUpdate bookingUpdate) {

        return ResponseEntity.ok(bookingService.updateBooking(loggedUser, id, bookingUpdate));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal User loggedUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(loggedUser, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
