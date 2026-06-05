package com.bookfair.backend.dto.common;

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
public class LayoutMarkerDto {
    @NotNull(message = "Id is required")
    private UUID id;

    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "Primary marker is required")
    private Boolean primaryMarker;

    @NotNull(message = "Active is required")
    private Boolean active;
    
    @NotNull(message = "Layout is required")
    private LayoutPositionDto layout;
}
