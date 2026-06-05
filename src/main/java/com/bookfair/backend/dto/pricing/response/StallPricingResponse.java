package com.bookfair.backend.dto.pricing.response;

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
public class StallPricingResponse {
    private UUID id;
    private UUID stallId;
    private String stallName;
    private String hallName;
    private BigDecimal basePrice;
    private BigDecimal manualOverridePrice;
    private BigDecimal finalPrice;
    private String status;
}
