package com.bookfair.backend.controller;

import com.bookfair.backend.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bookfair.backend.config.StripeProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;

    private final StripeProperties stripeProperties;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload, // CRITICAL: Must be raw String
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            // Verifying the signature and construct the event
            // This throws an exception if the payload was altered or signature is invalid
            // need to hande it
            Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhook().getSecret());

            // Passing the validated payload to your PaymentService logic
            paymentService.processWebhook(payload, sigHeader, "STRIPE");

            return ResponseEntity.ok("Success");

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
        }
    }
}