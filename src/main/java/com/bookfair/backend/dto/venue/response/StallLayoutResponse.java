package com.bookfair.backend.dto.venue.response;

import java.util.UUID;

import com.bookfair.backend.dto.common.LayoutPositionDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StallLayoutResponse {
    private UUID id;
    private String name;
    private String stallType;
    private LayoutPositionDto layout;
    private Boolean active;

}
