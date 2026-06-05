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
}
