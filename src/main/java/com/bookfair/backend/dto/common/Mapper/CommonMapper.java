package com.bookfair.backend.dto.common.Mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.LayoutMarkerDto;
import com.bookfair.backend.dto.common.LayoutPositionDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.model.LayoutMarker;
import com.bookfair.backend.model.LayoutPosition;

@Mapper(config = GlobalMapperConfig.class)
public interface CommonMapper {
    LayoutPositionDto toLayoutPositionDto(LayoutPosition layoutPosition);

    LayoutMarkerDto toLayoutMarkerDto(LayoutMarker marker);

    List<LayoutMarkerDto> toLayoutMarkerDtos(List<LayoutMarker> markers);

    LayoutPosition toLayoutPosition(LayoutPositionDto dto);

    LayoutPosition toLayoutPositionFromCoords(Integer xCoord, Integer yCoord, Integer width, Integer height);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "active", constant = "true")
    @org.mapstruct.Mapping(target = "type", source = "request.type")
    @org.mapstruct.Mapping(target = "label", source = "request.label")
    @org.mapstruct.Mapping(target = "primaryMarker", source = "request.primaryMarker")
    @org.mapstruct.Mapping(target = "layout", source = "layout")
    @org.mapstruct.Mapping(target = "venue", source = "venue")
    @org.mapstruct.Mapping(target = "building", source = "building")
    @org.mapstruct.Mapping(target = "hall", source = "hall")
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    LayoutMarker toLayoutMarker(com.bookfair.backend.dto.layout.request.CreateLayoutMarkerRequest request, LayoutPosition layout, com.bookfair.backend.model.Venue venue, com.bookfair.backend.model.Building building, com.bookfair.backend.model.Hall hall);
}

