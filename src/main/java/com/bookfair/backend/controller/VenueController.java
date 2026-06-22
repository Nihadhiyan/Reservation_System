package com.bookfair.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.building.response.BuildingResponse;
import com.bookfair.backend.dto.common.ApiResponseDto;
import com.bookfair.backend.dto.venue.request.CreateVenueRequest;
import com.bookfair.backend.dto.venue.request.UpdateVenueRequest;
import com.bookfair.backend.dto.venue.response.VenueMapResponse;
import com.bookfair.backend.dto.venue.response.VenueResponse;
import com.bookfair.backend.service.VenueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponseDto<VenueResponse>> createVenue(@RequestBody @Valid CreateVenueRequest request) {
        VenueResponse response = venueService.createVenue(request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Venue created successfully", response, LocalDateTime.now()));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<VenueResponse>>> getAllVenues(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<VenueResponse> response = venueService.getAllVenues(pageable);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Venues retrieved successfully", response, LocalDateTime.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<VenueResponse>> getVenue(@PathVariable UUID id) {
        VenueResponse response = venueService.getVenue(id);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Venue retrieved successfully", response, LocalDateTime.now()));
    }

    @GetMapping("/{id}/map")
    public ResponseEntity<ApiResponseDto<VenueMapResponse>> getVenueMap(@PathVariable UUID id) {
        VenueMapResponse response = venueService.getVenueMap(id);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Venue map retrieved successfully", response, LocalDateTime.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<VenueResponse>> updateVenue(@PathVariable UUID id,
            @Valid @RequestBody UpdateVenueRequest request) {
        VenueResponse response = venueService.updateVenue(id, request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Venue updated successfully", response, LocalDateTime.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteVenue(@PathVariable UUID id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Venue deleted successfully", null, LocalDateTime.now()));
    }

    @GetMapping("/{venueId}/buildings")
    public ResponseEntity<ApiResponseDto<List<BuildingResponse>>> getBuildingsByVenue(@PathVariable UUID venueId) {
        List<BuildingResponse> response = venueService.getBuildingsByVenue(venueId);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Buildings retrieved successfully", response, LocalDateTime.now()));
    }

    @GetMapping("/{venueId}/markers")
    public ResponseEntity<ApiResponseDto<Object>> getVenueMarkers(@PathVariable UUID venueId) {
        Object response = venueService.getMarkersByVenue(venueId);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Markers retrieved successfully", response, LocalDateTime.now()));
    }

}
