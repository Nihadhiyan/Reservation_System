package com.bookfair.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.reservation.request.CreateReservationRequest;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.service.ReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservationById(reservationId));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest reservationRequest) {
        return ResponseEntity.ok(reservationService.createReservation(reservationRequest));
    }

    @GetMapping("/publisher/{userId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUser(userId));
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<String> confirmReservation(@PathVariable UUID reservationId) {
        reservationService.confirmReservation(reservationId);
        return ResponseEntity.ok("Payment confirmed. Ticket generated and emailed to vendor.");
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<String> cancelReservation(@PathVariable UUID reservationId) {
        reservationService.requestCancellation(reservationId);
        return ResponseEntity.ok("Cancellation requested. Pending admin approval for refund.");
    }

    @PostMapping("/{reservationId}/refund")
    public ResponseEntity<String> approveRefund(@PathVariable UUID reservationId) {
        reservationService.approveRefund(reservationId);
        return ResponseEntity.ok("Refund processed successfully. Stalls have been released.");
    }
    
}
