package com.bookfair.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.payment.request.CreatePaymentRequest;
import com.bookfair.backend.dto.payment.response.PaymentResponse;
import com.bookfair.backend.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('USER', 'ORG_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> initializePayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initializePayment(request));
    }

    @PostMapping("/webhook")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> processWebhook(@RequestBody CreatePaymentRequest request) {
        paymentService.processWebhook(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{transactionId}/status")
    @PreAuthorize("hasAnyRole('USER', 'ORG_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(transactionId));
    }
}
