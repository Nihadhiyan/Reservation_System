package com.bookfair.backend.dto.event.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventStallRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Stall ID is required")
    private UUID stallId;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;

    @NotNull(message = "Manual override price is required")
    private BigDecimal manualOverridePrice;

    @NotNull(message = "Stall status is required")
    private String status;
}
