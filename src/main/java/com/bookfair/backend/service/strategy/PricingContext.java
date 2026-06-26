package com.bookfair.backend.service.strategy;

import java.time.LocalDateTime;

public record PricingContext(
        int durationDays,
        String orgType,
        LocalDateTime eventStartDate
) {}
