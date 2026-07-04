package com.bookfair.backend.dto.venue.response;

import java.util.List;
import java.util.UUID;

import com.bookfair.backend.dto.building.response.BuildingResponse;
import com.bookfair.backend.dto.common.LayoutMarkerDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueMapResponse implements Serializable {
    private UUID id;
    private String name;
    private String address;
    private List<LayoutMarkerDto> markers;
    private List<BuildingResponse> buildings;

}
