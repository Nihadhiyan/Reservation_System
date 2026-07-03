package com.bookfair.backend.dto.hall.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.hall.request.CreateHallRequest;
import com.bookfair.backend.dto.hall.request.UpdateHallRequest;
import com.bookfair.backend.dto.hall.response.HallLayoutResponse;
import com.bookfair.backend.dto.hall.response.HallResponse;
import com.bookfair.backend.model.Hall;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {CommonMapper.class}
)
public interface HallMapper {
    HallResponse toHallResponse(Hall hall);

    HallLayoutResponse toHallLayoutResponse(Hall hall);

    Hall toHallFromCreateHallRequest(CreateHallRequest request);

    Hall UpdateHallFromHallRequest(UpdateHallRequest request, @MappingTarget Hall hall);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "active", constant = "true")
    @org.mapstruct.Mapping(target = "floor", source = "floor")
    @org.mapstruct.Mapping(target = "layout", source = "request.layout")
    @org.mapstruct.Mapping(target = "name", source = "request.name")
    @org.mapstruct.Mapping(target = "spaceCategory", source = "request.spaceCategory")
    @org.mapstruct.Mapping(target = "hallType", source = "request.hallType")
    @org.mapstruct.Mapping(target = "blueprintImageUrl", source = "request.blueprintImageUrl")
    @org.mapstruct.Mapping(target = "squareFootage", source = "request.squareFootage")
    @org.mapstruct.Mapping(target = "maxStalls", source = "request.maxStalls")
    @org.mapstruct.Mapping(target = "wifiAvailable", source = "request.wifiAvailable")
    @org.mapstruct.Mapping(target = "airConditioned", source = "request.airConditioned")
    Hall toHall(CreateHallRequest request, com.bookfair.backend.model.Floor floor);
}

