package com.bookfair.backend.integration.payment.gateways;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.bookfair.backend.config.StripeProperties;

import com.bookfair.backend.dto.payment.request.CreatePaymentRequest;
import com.bookfair.backend.dto.payment.response.PaymentResponse;
import com.bookfair.backend.integration.payment.PaymentGateway;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData;

@Component
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties stripeProperties;

    @Override
    public PaymentResponse initializePayment(CreatePaymentRequest request) {
        try {
            Stripe.apiKey = stripeProperties.getApi().getKey();

            long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            String productName = "Reservation ID: " + request.getReservationId();

            List<LineItem> lineItems = Arrays.asList(
                    new LineItem.Builder()
                            .setQuantity(1L)
                            .setPriceData(PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(ProductData.builder()
                                            .setName(productName)
                                            .build())
                                    .build())
                            .build());

            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:5173/booking-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:5173/booking-cancel")
                    .addAllLineItem(lineItems)
                    .putMetadata("reservationId", request.getReservationId().toString())
                    .build();

            Session session = Session.create(sessionParams);

            PaymentResponse response = new PaymentResponse();
            response.setGateway("STRIPE");
            response.setTransactionId(session.getId());
            response.setPaymentUrl(session.getUrl());
            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Stripe initialization failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String gatewayType) {
        return "STRIPE".equalsIgnoreCase(gatewayType);
    }

    @Override
    public PaymentWebhookResult processWebhook(String payload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhook().getSecret());

            if ("checkout.session.completed".equals(event.getType())) {
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                if (dataObjectDeserializer.getObject().isPresent()) {
                    StripeObject stripeObject = dataObjectDeserializer.getObject().orElseThrow(() -> new IllegalStateException("Deserialized object missing"));
                    if (stripeObject instanceof Session session) {
                        UUID reservationId = UUID.fromString(session.getMetadata().get("reservationId"));
                        BigDecimal amount = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));
                        String paymentStatus = "COMPLETED";
                        
                        return new PaymentWebhookResult(true, session.getId(), paymentStatus, amount, reservationId);
                    }
                }
            }
            
            return new PaymentWebhookResult(true, null, "IGNORED", null, null);
        } catch (SignatureVerificationException e) {
            System.err.println("Webhook verification failed: " + e.getMessage());
            return new PaymentWebhookResult(false, null, "FAILED", null, null);
        }
    }
}
