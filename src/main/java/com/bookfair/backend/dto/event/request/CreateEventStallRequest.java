package com.bookfair.backend.dto.event.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventStallRequest {

    @NotBlank(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Stall ID is required")
    private UUID stallId;

    @NotBlank(message = "Base price is required")
    private BigDecimal basePrice;

    @NotBlank(message = "Manual override price is required")
    private BigDecimal manualOverridePrice;

    @NotBlank(message = "Stall status is required")
    private String status;
}
