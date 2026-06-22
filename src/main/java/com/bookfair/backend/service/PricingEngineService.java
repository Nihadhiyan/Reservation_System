package com.bookfair.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Stall.StallType;

@Service
public class PricingEngineService {
    public BigDecimal calculateFinalPrice(EventStall eventStall) {
        
        if (eventStall == null) {
            return BigDecimal.ZERO;
        }

        if(eventStall.getManualOverridePrice() != null && 
           eventStall.getManualOverridePrice().compareTo(BigDecimal.ZERO) > 0) {
            
            return eventStall.getManualOverridePrice();

        }

        BigDecimal basePrice = eventStall.getBasePrice() != null 
            ? eventStall.getBasePrice()
            : BigDecimal.ZERO;

        StallType stallType = (eventStall.getStall() != null && eventStall.getStall().getStallType() != null)
            ? eventStall.getStall().getStallType()
            : StallType.STANDARD;
        
        BigDecimal multiplier = getMultiplierForStallType(stallType);

        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

    }

    private BigDecimal getMultiplierForStallType(StallType stallType) {

        return switch (stallType) {
            case SPONSOR -> new BigDecimal("2.00"); // 100% markup (Double the base price)
            case PREMIUM -> new BigDecimal("1.50"); // 50% markup
            case ISLAND  -> new BigDecimal("1.30"); // 30% markup
            case CORNER  -> new BigDecimal("1.15"); // 15% markup
            case STANDARD -> new BigDecimal("1.00"); // No markup
            default -> new BigDecimal("1.00");
        };
    }

    public Long convertToCents(BigDecimal finalPrice) {
        if (finalPrice == null) {
            return 0L;
        }

        return finalPrice.multiply(new BigDecimal("100")).longValueExact();
    }
}
