package com.bookfair.backend.dto.stall.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.stall.request.CreateStallRequest;
import com.bookfair.backend.dto.stall.request.UpdateStallRequest;
import com.bookfair.backend.dto.stall.response.StallLayoutResponse;
import com.bookfair.backend.dto.stall.response.StallResponse;
import com.bookfair.backend.model.Stall;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {CommonMapper.class}
)
public interface StallMapper {
    StallResponse toStallResponse(Stall stall);

    Stall toStallFromCreateStallRequest(CreateStallRequest request);

    Stall updateStallFromStallRequest(UpdateStallRequest request, @MappingTarget Stall stall);

    StallLayoutResponse toStallLayoutResponse(Stall stall);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "active", constant = "true")
    @org.mapstruct.Mapping(target = "hall", source = "hall")
    @org.mapstruct.Mapping(target = "name", source = "name")
    @org.mapstruct.Mapping(target = "squareFootage", source = "squareFootage")
    @org.mapstruct.Mapping(target = "layout", source = "layout")
    Stall toGeneratedStall(com.bookfair.backend.model.Hall hall, String name, Double squareFootage, com.bookfair.backend.model.LayoutPosition layout);
}

