package com.bookfair.backend.integration.payment;

import java.math.BigDecimal;
import java.util.UUID;

import com.bookfair.backend.dto.payment.request.CreatePaymentRequest;
import com.bookfair.backend.dto.payment.response.PaymentResponse;

public interface PaymentGateway {

    // Initializes a checkout session or payment intent with the external provider.
    PaymentResponse initializePayment(CreatePaymentRequest request);

    // Verifies if this adapter supports the requested gateway (e.g., "STRIPE", "PAYPAL").
    boolean supports(String gatewayType);

    // Abstracts webhook processing to return a generic result
    PaymentWebhookResult processWebhook(String payload, String signatureHeader);

    public record PaymentWebhookResult(boolean isValid, String transactionId, String paymentStatus, BigDecimal amount, UUID reservationId) {}
}