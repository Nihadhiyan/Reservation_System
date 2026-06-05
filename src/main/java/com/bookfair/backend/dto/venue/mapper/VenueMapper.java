package com.bookfair.backend.dto.venue.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.LayoutMarkerDto;
import com.bookfair.backend.dto.common.LayoutPositionDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.venue.request.CreateStallRequest;
import com.bookfair.backend.dto.venue.request.UpdateStallRequest;
import com.bookfair.backend.dto.venue.response.BuildingResponse;
import com.bookfair.backend.dto.venue.response.FloorResponse;
import com.bookfair.backend.dto.venue.response.HallLayoutResponse;
import com.bookfair.backend.dto.venue.response.HallResponse;
import com.bookfair.backend.dto.venue.response.StallLayoutResponse;
import com.bookfair.backend.dto.venue.response.StallResponse;
import com.bookfair.backend.dto.venue.response.VenueMapResponse;
import com.bookfair.backend.dto.venue.response.VenueResponse;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutMarker;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.Venue;

@Mapper(config = GlobalMapperConfig.class)
public interface VenueMapper {
    VenueResponse toVenueResponse(Venue venue);

    BuildingResponse toBuildingResponse(Building building);

    FloorResponse toFloorResponse(Floor floor);

    HallResponse toHallResponse(Hall hall);

    StallResponse toStallResponse(Stall stall);

    Stall toStallFromCreateStallRequest(CreateStallRequest request);

    Stall updateStallFromStallRequest(UpdateStallRequest request, @MappingTarget Stall stall);

    VenueMapResponse toVenueMapResponse(Venue venue);

    HallLayoutResponse toHallLayoutResponse(Hall hall);

    StallLayoutResponse toStallLayoutResponse(Stall stall);

    LayoutPositionDto toLayoutPositionDto(LayoutPosition layoutPosition);

    LayoutMarkerDto toLayoutMarkerDto(LayoutMarker marker);

    List<LayoutMarkerDto> toLayoutMarkerDtos(List<LayoutMarker> markers);
}
