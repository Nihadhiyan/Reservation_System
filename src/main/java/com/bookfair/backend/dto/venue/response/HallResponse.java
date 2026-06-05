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
public class HallResponse {
    private UUID id;
    private String name;
    private String hallType;
    private LayoutPositionDto layout;
    private String blueprintImageUrl;
    private Double squareFootage;
    private Boolean active;
    private Integer maxStalls;
    private Integer currentStallCount;
    private Boolean wifiAvailable;
    private Boolean airConditioned;
}
