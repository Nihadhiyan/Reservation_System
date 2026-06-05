package com.bookfair.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LayoutPositionDto {
    @NotNull(message = "X coord is required")
    private Integer xCoord;

    @NotNull(message = "Y coord is required")
    private Integer yCoord;

    @NotNull(message = "Width is required")
    private Integer width;
    
    @NotNull(message = "Height is required")
    private Integer height;
}
