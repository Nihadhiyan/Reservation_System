package com.bookfair.backend.service.strategy;

import java.math.BigDecimal;

public interface PricingStrategy {
    boolean matches(String conditionValue, PricingContext context);
    BigDecimal apply(BigDecimal currentPrice, BigDecimal multiplier);
}
