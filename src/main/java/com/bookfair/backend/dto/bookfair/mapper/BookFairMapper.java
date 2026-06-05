package com.bookfair.backend.dto.bookfair.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.bookfair.response.BookFairResponse;
import com.bookfair.backend.dto.bookfair.response.BookFairStallResponse;
import com.bookfair.backend.dto.bookfair.response.BookFairSummaryResponse;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.BookFairStall;

@Mapper(config = GlobalMapperConfig.class)
public interface BookFairMapper {
    BookFairResponse toBookFairResponse(BookFair bookFair);

    BookFairStallResponse toBookFairStallResponse(BookFairStall bookFairStall);

    BookFairSummaryResponse toBookFairSummaryResponse(BookFair bookFair);
}
