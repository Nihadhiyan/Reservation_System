package com.bookfair.backend.dto.pricing.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.SimpleEventDto;
import com.bookfair.backend.dto.common.SimpleStallDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.pricing.response.StallPricingResponse;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Stall;

@Mapper(config = GlobalMapperConfig.class)
public interface PricingMapper {
    SimpleEventDto toSimpleEventDto(Event event);

    SimpleStallDto toSimpleStallDto(Stall stall);

    StallPricingResponse toStallPricingResponse(EventStall eventStall);
}
