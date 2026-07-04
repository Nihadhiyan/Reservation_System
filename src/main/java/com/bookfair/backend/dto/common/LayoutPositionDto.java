package com.bookfair.backend.dto.common;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Implements Serializable for Redis caching compatibility
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LayoutPositionDto implements Serializable {
    private Integer xCoord;
    private Integer yCoord;
    private Integer width;
    private Integer height;
    // private Integer rotation;
    // private Integer zIndex;
}
