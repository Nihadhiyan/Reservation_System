package com.bookfair.backend.dto.venue.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
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
    private String mapImageUrl;
    private String blueprintImageUrl;
    private Double totalSquareFootage;
    private Boolean parkingAvailable;
    private Boolean foodCourtAvailable;
    private Boolean active;
}
