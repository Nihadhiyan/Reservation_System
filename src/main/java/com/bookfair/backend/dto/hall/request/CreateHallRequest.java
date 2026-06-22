package com.bookfair.backend.dto.hall.request;

import java.util.UUID;

import com.bookfair.backend.dto.common.LayoutPositionDto;
import com.bookfair.backend.model.Hall;

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
public class CreateHallRequest {
    
    @NotNull(message = "Floor Id is required")
    private UUID floorId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Space category is required")
    private Hall.SpaceCategory spaceCategory;

    @NotNull(message = "Hall type is required")
    private Hall.HallType hallType;

    @Valid
    @NotNull(message = "Layout is required")
    private LayoutPositionDto layout;

    @NotBlank(message = "Blueprint image url is required")
    private String blueprintImageUrl;

    @NotNull(message = "Square footage is required")
    private Double squareFootage;

    @NotNull(message = "Max stalls is required")
    private Integer maxStalls;

    @NotNull(message = "Wifi available is required")
    private Boolean wifiAvailable;
    
    @NotNull(message = "Air conditioned is required")
    private Boolean airConditioned;
}
