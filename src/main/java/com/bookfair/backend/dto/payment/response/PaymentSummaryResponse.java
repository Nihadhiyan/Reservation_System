package com.bookfair.backend.dto.payment.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private UUID id;
    private UUID reservationId;
    private BigDecimal amount;
    private String status;









}
