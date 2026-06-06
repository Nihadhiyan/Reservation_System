package com.bookfair.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.bookfair.backend.model.BookFairStall;
import com.bookfair.backend.model.Stall.StallType;

@Service
public class PricingEngineService {
    public BigDecimal calculateFinalPrice(BookFairStall bookFairStall) {
        
        if (bookFairStall == null) {
            return BigDecimal.ZERO;
        }

        if(bookFairStall.getManualOverridePrice() != null && 
           bookFairStall.getManualOverridePrice().compareTo(BigDecimal.ZERO) > 0) {
            
            return bookFairStall.getManualOverridePrice();

        }

        BigDecimal basePrice = bookFairStall.getBasePrice() != null 
            ? bookFairStall.getBasePrice()
            : BigDecimal.ZERO;

        StallType stallType = (bookFairStall.getStall() != null && bookFairStall.getStall().getStallType() != null)
            ? bookFairStall.getStall().getStallType()
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
