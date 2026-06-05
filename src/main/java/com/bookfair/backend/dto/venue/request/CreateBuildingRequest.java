package com.bookfair.backend.dto.venue.request;

import java.util.UUID;

import com.bookfair.backend.dto.common.LayoutPositionDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuildingRequest {
    @NotNull(message = "Venue id is required")
    private UUID venueId;

    @NotBlank(message = "Name is required")
    private String name;

    @Valid
    @NotNull(message = "Layout is required")
    private LayoutPositionDto layout;

    @NotNull(message = "Square footage is required")
    private Double squareFootage;

    @NotNull(message = "Number of floors is required")
    private Integer numberOfFloors;
    
    @NotBlank(message = "Type is required")
    private String type;

}
