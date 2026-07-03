package com.bookfair.backend.dto.pricing.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.SimpleEventDto;
import com.bookfair.backend.dto.common.SimpleStallDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.pricing.request.PricingRuleRequest;
import com.bookfair.backend.dto.pricing.response.PricingRuleResponse;
import com.bookfair.backend.dto.pricing.response.StallPricingResponse;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.PricingRule;
import com.bookfair.backend.model.Stall;

@Mapper(config = GlobalMapperConfig.class)
public interface PricingMapper {
    SimpleEventDto toSimpleEventDto(Event event);

    SimpleStallDto toSimpleStallDto(Stall stall);

    StallPricingResponse toStallPricingResponse(EventStall eventStall);

    PricingRuleResponse toPricingRuleResponse(PricingRule pricingRule);

    PricingRule toPricingRule(PricingRuleRequest request);

    @org.mapstruct.Mapping(target = "stallId", source = "stallId")
    @org.mapstruct.Mapping(target = "stallName", source = "stallName")
    @org.mapstruct.Mapping(target = "basePrice", source = "basePrice")
    @org.mapstruct.Mapping(target = "finalPrice", source = "finalPrice")
    StallPricingResponse toStallPricingResponse(java.util.UUID stallId, String stallName, java.math.BigDecimal basePrice, java.math.BigDecimal finalPrice);

    @org.mapstruct.Mapping(target = "stalls", source = "stalls")
    @org.mapstruct.Mapping(target = "subtotal", source = "subtotal")
    @org.mapstruct.Mapping(target = "discountAmount", source = "discountAmount")
    @org.mapstruct.Mapping(target = "taxAmount", source = "taxAmount")
    @org.mapstruct.Mapping(target = "total", source = "total")
    @org.mapstruct.Mapping(target = "currency", source = "currency")
    com.bookfair.backend.dto.pricing.response.PricingBreakdownResponse toPricingBreakdownResponse(
            java.util.List<StallPricingResponse> stalls,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal discountAmount,
            java.math.BigDecimal taxAmount,
            java.math.BigDecimal total,
            String currency);
}

