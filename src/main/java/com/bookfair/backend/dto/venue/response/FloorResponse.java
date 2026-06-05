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
public class FloorResponse {
    private UUID id;
    private String levelName;
    private Integer levelNumber;
}
