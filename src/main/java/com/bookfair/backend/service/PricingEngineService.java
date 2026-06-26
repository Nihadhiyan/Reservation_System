package com.bookfair.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.pricing.response.PricingBreakdownResponse;
import com.bookfair.backend.dto.pricing.response.StallPricingResponse;
import com.bookfair.backend.model.PricingRule;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.PricingRuleRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.service.strategy.PricingContext;
import com.bookfair.backend.service.strategy.PricingStrategy;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingEngineService {

    private final PricingRuleRepository pricingRuleRepository;
    private final StallRepository stallRepository;
    private final Map<String, PricingStrategy> strategies;

    @Transactional(readOnly = true)
    public PricingBreakdownResponse calculateQuote(List<UUID> stallIds, int durationDays, String orgType) {
        requireNonNull(stallIds, "stallIds cannot be null");
        List<Stall> stalls = stallRepository.findAllById(stallIds);

        List<PricingRule> activeRules = pricingRuleRepository.findAllByActiveTrue();
        activeRules.sort(
                java.util.Comparator.comparing(r -> r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE));

        PricingContext context = new PricingContext(durationDays, orgType, null);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<StallPricingResponse> stallPricings = new ArrayList<>();

        for (Stall stall : stalls) {
            BigDecimal basePrice = BigDecimal.valueOf(stall.getSquareFootage()).multiply(BigDecimal.valueOf(10.0))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal currentPrice = basePrice.multiply(BigDecimal.valueOf(durationDays));

            for (PricingRule rule : activeRules) {
                PricingStrategy strategy = strategies.get(rule.getConditionType().name());
                if (strategy != null && strategy.matches(rule.getConditionValue(), context)) {
                    currentPrice = strategy.apply(currentPrice, rule.getMultiplier());
                }
            }
            currentPrice = currentPrice.setScale(2, java.math.RoundingMode.HALF_UP);

            subtotal = subtotal.add(currentPrice);

            StallPricingResponse spr = new StallPricingResponse();
            spr.setStallId(stall.getId());
            spr.setStallName(stall.getName());
            spr.setBasePrice(basePrice);
            spr.setFinalPrice(currentPrice);
            stallPricings.add(spr);
        }

        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmount).add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        PricingBreakdownResponse response = new PricingBreakdownResponse();
        response.setStalls(stallPricings);
        response.setSubtotal(subtotal);
        response.setDiscountAmount(discountAmount);
        response.setTaxAmount(taxAmount);
        response.setTotal(total);
        response.setCurrency("USD");

        return response;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(com.bookfair.backend.model.EventStall eventStall) {
        if (eventStall.getManualOverridePrice() != null) {
            return eventStall.getManualOverridePrice();
        }
        return eventStall.getBasePrice() != null ? eventStall.getBasePrice() : BigDecimal.valueOf(100.0);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalForReservation(com.bookfair.backend.model.Reservation reservation) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (reservation.getReservedStalls() != null) {
            for (com.bookfair.backend.model.ReservationStall rs : reservation.getReservedStalls()) {
                subtotal = subtotal.add(calculateFinalPrice(rs.getEventStall()));
            }
        }
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(2, java.math.RoundingMode.HALF_UP);
        return subtotal.subtract(discountAmount).add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateVenueRent(com.bookfair.backend.model.Venue venue, int durationDays) {
        if (venue.getRentType() == com.bookfair.backend.model.Venue.RentType.PERCENTAGE_OF_REVENUE) {
            // Placeholder: Initial liability is 0, grows dynamically later
            return BigDecimal.ZERO;
        }
        BigDecimal rate = venue.getDailyRentRate() != null ? venue.getDailyRentRate() : BigDecimal.ZERO;
        return rate.multiply(BigDecimal.valueOf(durationDays)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
