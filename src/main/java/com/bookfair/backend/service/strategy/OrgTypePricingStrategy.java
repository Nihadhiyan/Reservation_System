package com.bookfair.backend.service.strategy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("ORG_TYPE")
public class OrgTypePricingStrategy implements PricingStrategy {

    @Override
    public boolean matches(String conditionValue, PricingContext context) {
        return conditionValue != null && conditionValue.equalsIgnoreCase(context.orgType());
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, BigDecimal multiplier) {
        if (multiplier == null) return currentPrice;
        return currentPrice.multiply(multiplier);
    }
}
