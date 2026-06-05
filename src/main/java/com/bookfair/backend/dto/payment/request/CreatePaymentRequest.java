package com.bookfair.backend.dto.payment.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotNull(message = "Reservation id is required")
    private UUID reservationId;

    @NotBlank(message = "Stripe charge id is required")
    private String stripeChargeId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @NotBlank(message = "Status is required")
    private String status;









}
