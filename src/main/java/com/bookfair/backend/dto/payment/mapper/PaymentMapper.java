package com.bookfair.backend.dto.payment.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.payment.response.PaymentResponse;
import com.bookfair.backend.dto.payment.response.PaymentSummaryResponse;
import com.bookfair.backend.model.Payment;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);

    PaymentSummaryResponse toPaymentSummaryResponse(Payment payment);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "status", constant = "PENDING")
    @org.mapstruct.Mapping(target = "reservation", source = "reservation")
    @org.mapstruct.Mapping(target = "amount", source = "amount")
    @org.mapstruct.Mapping(target = "transactionId", source = "transactionId")
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    Payment toPayment(com.bookfair.backend.model.Reservation reservation, java.math.BigDecimal amount, String transactionId);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "status", ignore = true)
    @org.mapstruct.Mapping(target = "reservation", source = "reservation")
    @org.mapstruct.Mapping(target = "transactionId", source = "transactionId")
    @org.mapstruct.Mapping(target = "amount", source = "amount")
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    Payment toWebhookPayment(com.bookfair.backend.model.Reservation reservation, String transactionId, java.math.BigDecimal amount);
}

