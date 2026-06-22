package com.bookfair.backend.dto.venue.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid contact number format")
    private String contactNumber;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Website is required")
    @Pattern(regexp = "^(https?://)?(www\\.)?([a-zA-Z0-9]+\\.)?[a-zA-Z0-9]+\\.[a-zA-Z]{2,}(/\\S*)?$", message = "Website must be a valid URL")
    private String website;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Google Place Id is required")
    private String googlePlaceId;

    @NotBlank(message = "Map image url is required")
    private String mapImageUrl;

    @NotBlank(message = "Blueprint image url is required")
    private String blueprintImageUrl;

    @NotNull(message = "Total square footage is required")
    private Double totalSquareFootage;

    @NotNull(message = "Parking available is required")
    private Boolean parkingAvailable;
    
    @NotNull(message = "Food court available is required")
    private Boolean foodCourtAvailable;

    @NotNull(message = "Owner organization ID is required")
    private UUID ownerOrganizationId;

    private List<UUID> partnerOrganizationIds;
}
