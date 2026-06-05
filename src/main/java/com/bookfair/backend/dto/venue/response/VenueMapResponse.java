package com.bookfair.backend.dto.venue.response;

import java.util.List;
import java.util.UUID;

import com.bookfair.backend.dto.common.LayoutMarkerDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueMapResponse {
    private UUID id;
    private String name;
    private String address;
    private List<LayoutMarkerDto> markers;
    private List<BuildingResponse> buildings;

}
