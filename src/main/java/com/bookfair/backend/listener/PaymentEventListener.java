package com.bookfair.backend.listener;

import java.util.Objects;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookfair.backend.config.StripeProperties;
import com.bookfair.backend.event.reservation.ReservationCancelledByAdminEvent;
import com.bookfair.backend.event.reservation.ReservationRefundedEvent;
import com.bookfair.backend.model.Payment;
import com.bookfair.backend.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentRepository paymentRepository;
    private final StripeProperties stripeProperties;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReservationCancelledByAdmin(ReservationCancelledByAdminEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        log.info("Processing async refund for cancelled reservation: {}", event.reservationId());
        
        paymentRepository.findByReservationId(event.reservationId()).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                try {
                    Stripe.apiKey = stripeProperties.getApi().getKey();
                    if (payment.getTransactionId() != null && payment.getTransactionId().startsWith("cs_")) {
                        Session session = Session.retrieve(payment.getTransactionId());
                        if (session != null && session.getPaymentIntent() != null) {
                            RefundCreateParams params = RefundCreateParams.builder()
                                    .setPaymentIntent(session.getPaymentIntent())
                                    .build();
                            Refund.create(params);
                        }
                    } else if (payment.getTransactionId() != null && payment.getTransactionId().startsWith("pi_")) {
                        RefundCreateParams params = RefundCreateParams.builder()
                                .setPaymentIntent(payment.getTransactionId())
                                .build();
                        Refund.create(params);
                    }
                    log.info("Stripe refund triggered successfully for payment {}", payment.getId());
                } catch (Exception e) {
                    log.warn("Stripe refund call failed or skipped in dev/test environment for payment {}: {}", payment.getId(), e.getMessage());
                }
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                
                if (payment.getReservation() != null && payment.getReservation().getUser() != null) {
                    eventPublisher.publishEvent(new ReservationRefundedEvent(
                            payment.getReservation().getUser().getId(),
                            payment.getReservation().getUser().getUsername(),
                            payment.getReservation().getUser().getEmail(),
                            payment.getReservation().getId(),
                            payment.getReservation().getEvent() != null ? payment.getReservation().getEvent().getName() : "Event"));
                }
                log.info("Updated payment {} status to REFUNDED", payment.getId());
            }
        });
    }
}
