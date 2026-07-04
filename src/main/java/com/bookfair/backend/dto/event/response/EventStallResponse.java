package com.bookfair.backend.dto.event.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.bookfair.backend.dto.common.LayoutPositionDto;

// Implements Serializable for Redis caching compatibility
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventStallResponse implements Serializable {
    private UUID id;
    private UUID eventId;
    private UUID stallId;
    private String stallName;
    private String hallName;
    private BigDecimal basePrice;
    private BigDecimal manualOverridePrice;
    private String status;
    private LayoutPositionDto layout;

}
