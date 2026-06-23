package com.bookfair.backend.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.payment.mapper.PaymentMapper;
import com.bookfair.backend.dto.payment.request.CreatePaymentRequest;
import com.bookfair.backend.dto.payment.response.PaymentResponse;
import com.bookfair.backend.event.payment.PaymentCompletedEvent;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Payment;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.repository.PaymentRepository;
import com.bookfair.backend.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse initializePayment(CreatePaymentRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(request.getAmount());
        payment.setStatus(Payment.PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("Initialized payment for reservation {}", reservation.getId());

        return paymentMapper.toPaymentResponse(saved);
    }

    @Transactional
    public void processWebhook(CreatePaymentRequest request) {
        // Validate payload and extract transaction state
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setStripeChargeId(request.getStripeChargeId());
        payment.setAmount(request.getAmount());
        
        Payment.PaymentStatus status = Payment.PaymentStatus.valueOf(request.getStatus().toUpperCase());
        payment.setStatus(status);

        Payment saved = paymentRepository.save(payment);
        log.info("Processed webhook for payment {}", saved.getId());

        if (status == Payment.PaymentStatus.COMPLETED) {
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    reservation.getId(),
                    saved.getStripeChargeId(),
                    saved.getAmount()
            ));
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID transactionId) {
        Payment payment = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found", ErrorCode.BUSINESS_RULE_VIOLATION));

        return paymentMapper.toPaymentResponse(payment);
    }
}
