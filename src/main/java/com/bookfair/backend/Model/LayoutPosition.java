package com.bookfair.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LayoutPosition {

    @Column(name = "x_coord", nullable = false)
    @Min(value = 0, message = "X coordinate must be non-negative")
    private Integer xCoord;

    @Column(name = "y_coord", nullable = false)
    @Min(value = 0, message = "Y coordinate must be non-negative")
    private Integer yCoord;

    @Column(nullable = false)
    @Min(value = 0, message = "Width must be non-negative")
    private Integer width;

    @Column(nullable = false)
    @Min(value = 0, message = "Height must be non-negative")
    private Integer height;

    // @Column(name = "latitude", precision = 9, scale = 6)
    // private Double latitude;

    // @Column(name = "longitude", precision = 9, scale = 6)
    // private Double longitude;
    
}
