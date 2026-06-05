package com.bookfair.backend.dto.venue.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFloorRequest {
    @NotNull(message = "Building id is required")
    private UUID buildingId;

    @NotBlank(message = "Level name is required")
    private String levelName;
    
    @NotNull(message = "Level number is required")
    private Integer levelNumber;

}
