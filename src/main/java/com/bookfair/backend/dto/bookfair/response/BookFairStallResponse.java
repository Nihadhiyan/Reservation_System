package com.bookfair.backend.dto.bookfair.response;

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
public class BookFairStallResponse {
    private UUID id;
    private UUID bookFairId;
    private UUID stallId;
    private String stallName;
    private String hallName;
    private BigDecimal basePrice;
    private BigDecimal manualOverridePrice;
    private String status;

















}
