package com.bookfair.backend.dto.venue.response;

import java.util.List;
import java.util.UUID;

import com.bookfair.backend.dto.common.SimpleOrganizationDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse implements Serializable {
    private UUID id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private String contactNumber;
    private String email;
    private String website;
    private Double latitude;
    private Double longitude;
    private String googlePlaceId;
    private String mapImageUrl;
    private String blueprintImageUrl;
    private Double totalSquareFootage;
    private Boolean parkingAvailable;
    private Boolean foodCourtAvailable;
    private SimpleOrganizationDto owner;
    private List<SimpleOrganizationDto> partners;
    private Boolean active;
}
