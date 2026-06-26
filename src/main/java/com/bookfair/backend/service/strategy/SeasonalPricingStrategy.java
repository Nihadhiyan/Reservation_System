package com.bookfair.backend.service.strategy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("SEASONAL")
public class SeasonalPricingStrategy implements PricingStrategy {

    @Override
    public boolean matches(String conditionValue, PricingContext context) {
        if (conditionValue == null || context.eventStartDate() == null) return false;
        
        int month = context.eventStartDate().getMonthValue();
        
        if (conditionValue.equalsIgnoreCase("SUMMER")) {
            return month >= 6 && month <= 8; // June, July, August
        }
        
        // Add more seasonal rules here
        return false;
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, BigDecimal multiplier) {
        if (multiplier == null) return currentPrice;
        return currentPrice.multiply(multiplier);
    }
}
