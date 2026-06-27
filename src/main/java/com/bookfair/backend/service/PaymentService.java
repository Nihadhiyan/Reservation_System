package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.payment.mapper.PaymentMapper;
import com.bookfair.backend.dto.payment.request.CreatePaymentRequest;
import com.bookfair.backend.dto.payment.response.PaymentResponse;
import com.bookfair.backend.event.payment.PaymentCompletedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.integration.payment.PaymentGateway;
import com.bookfair.backend.integration.payment.PaymentGateway.PaymentWebhookResult;
import com.bookfair.backend.model.Payment;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.repository.PaymentRepository;
import com.bookfair.backend.repository.ReservationRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PricingEngineService pricingEngineService;
    private final PaymentMapper paymentMapper;
    private final List<PaymentGateway> paymentGateways;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse initializePayment(CreatePaymentRequest request, String gatewayType) {
        requireNonNull(request, "request cannot be null");
        Reservation reservation = reservationRepository.findById(requireNonNull(request.getReservationId()))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        java.math.BigDecimal calculatedTotal = pricingEngineService.calculateTotalForReservation(reservation);
        if (request.getAmount().compareTo(calculatedTotal) != 0) {
            throw new BusinessException(
                    "Price mismatch: requested " + request.getAmount() + " but calculated " + calculatedTotal,
                    ErrorCode.PRICE_MISMATCH);
        }

        PaymentGateway adapter = paymentGateways.stream()
                .filter(gateway -> gateway.supports(requireNonNull(gatewayType)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Unsupported payment gateway: " + gatewayType,
                        ErrorCode.BUSINESS_RULE_VIOLATION));

        PaymentResponse response = adapter.initializePayment(request);

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(request.getAmount());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId(response.getTransactionId());

        Payment saved = paymentRepository.save(payment);
        log.info("Initialized payment for reservation {} via {}", reservation.getId(), gatewayType);

        return paymentMapper.toPaymentResponse(saved);
    }

    @Transactional
    public void processWebhook(String payload, String signatureHeader, String gatewayType) {
        PaymentGateway adapter = paymentGateways.stream()
                .filter(gateway -> gateway.supports(requireNonNull(gatewayType)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Unsupported payment gateway: " + gatewayType,
                        ErrorCode.BUSINESS_RULE_VIOLATION));

        PaymentWebhookResult result = adapter.processWebhook(payload, signatureHeader);

        if (!result.isValid() || result.transactionId() == null) {
            log.warn("Ignored or invalid webhook from gateway {}", gatewayType);
            return;
        }

        Payment payment = paymentRepository.findByTransactionId(requireNonNull(result.transactionId()))
                .orElseGet(() -> {
                    // Fallback to searching by reservationId if transaction ID wasn't saved yet
                    Reservation reservation = reservationRepository.findById(requireNonNull(result.reservationId()))
                            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found",
                                    ErrorCode.RESERVATION_NOT_FOUND));

                    Payment newPayment = new Payment();
                    newPayment.setReservation(reservation);
                    newPayment.setTransactionId(result.transactionId());
                    newPayment.setAmount(result.amount());
                    return newPayment;
                });

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            log.info("Idempotency Check: Payment {} is already COMPLETED. Ignoring webhook.", payment.getId());
            return;
        }

        Payment.PaymentStatus status = Payment.PaymentStatus.valueOf(result.paymentStatus().toUpperCase());
        payment.setStatus(status);

        Payment saved = paymentRepository.save(payment);
        log.info("Processed webhook for payment {}", saved.getId());

        if (status == Payment.PaymentStatus.COMPLETED) {
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    requireNonNull(payment.getReservation().getId()),
                    requireNonNull(saved.getTransactionId()),
                    requireNonNull(saved.getAmount())));
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID transactionId) {
        Payment payment = paymentRepository.findById(requireNonNull(transactionId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Payment not found", ErrorCode.BUSINESS_RULE_VIOLATION));

        return paymentMapper.toPaymentResponse(payment);
    }
}
