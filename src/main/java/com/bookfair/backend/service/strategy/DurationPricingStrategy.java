package com.bookfair.backend.service.strategy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("DURATION")
public class DurationPricingStrategy implements PricingStrategy {

    @Override
    public boolean matches(String conditionValue, PricingContext context) {
        if (conditionValue == null) return false;
        
        if (conditionValue.equals(">7_DAYS")) {
            return context.durationDays() > 7;
        }
        
        // Add more duration rules here
        return false;
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, BigDecimal multiplier) {
        if (multiplier == null) return currentPrice;
        return currentPrice.multiply(multiplier);
    }
}
