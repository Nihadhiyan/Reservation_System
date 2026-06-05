package com.bookfair.backend.dto.pricing.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.SimpleBookFairDto;
import com.bookfair.backend.dto.common.SimpleStallDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.pricing.response.StallPricingResponse;
import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.BookFairStall;
import com.bookfair.backend.model.Stall;

@Mapper(config = GlobalMapperConfig.class)
public interface PricingMapper {
    SimpleBookFairDto toSimpleBookFairDto(BookFair bookFair);

    SimpleStallDto toSimpleStallDto(Stall stall);

    StallPricingResponse toStallPricingResponse(BookFairStall bookFairStall);
}
