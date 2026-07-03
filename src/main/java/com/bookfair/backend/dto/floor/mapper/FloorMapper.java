package com.bookfair.backend.dto.floor.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.floor.request.CreateFloorRequest;
import com.bookfair.backend.dto.floor.request.UpdateFloorRequest;
import com.bookfair.backend.dto.floor.response.FloorResponse;
import com.bookfair.backend.model.Floor;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {CommonMapper.class}
)
public interface FloorMapper {
    FloorResponse toFloorResponse(Floor floor);

    Floor toFloorFromCreateFloorRequest(CreateFloorRequest request);

    Floor UpdateFloorFromFloorRequest(UpdateFloorRequest request, @MappingTarget Floor floor);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "levelName", source = "request.levelName")
    @org.mapstruct.Mapping(target = "levelNumber", source = "request.levelNumber")
    @org.mapstruct.Mapping(target = "building", source = "building")
    Floor toFloor(CreateFloorRequest request, com.bookfair.backend.model.Building building);
}

