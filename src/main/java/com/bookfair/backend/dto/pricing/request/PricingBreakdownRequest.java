package com.bookfair.backend.dto.pricing.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingBreakdownRequest {
    @NotNull(message = "Reservation id is required")
    private UUID reservationId;

    @NotNull(message = "Discount amount is required")
    private BigDecimal discountAmount;
    
    @NotNull(message = "Tax amount is required")
    private BigDecimal taxAmount;







}
