package com.bookfair.backend.dto.venue.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.organization.mapper.OrganizationMapper;
import com.bookfair.backend.dto.venue.request.CreateVenueRequest;
import com.bookfair.backend.dto.venue.request.UpdateVenueRequest;
import com.bookfair.backend.dto.venue.response.VenueMapResponse;
import com.bookfair.backend.dto.venue.response.VenueResponse;
import com.bookfair.backend.model.Venue;

@Mapper(config = GlobalMapperConfig.class, uses = {
        CommonMapper.class,
        OrganizationMapper.class
})
public interface VenueMapper {
    VenueResponse toVenueResponse(Venue venue);

    VenueMapResponse toVenueMapResponse(Venue venue);

    Venue updateVenueFromUpdateVenueRequest(UpdateVenueRequest request, @MappingTarget Venue venue);

    Venue toVenueFromCreateVenueRequest(CreateVenueRequest request);
}
