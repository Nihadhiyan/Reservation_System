package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.building.mapper.BuildingMapper;
import com.bookfair.backend.dto.building.request.CreateBuildingRequest;
import com.bookfair.backend.dto.building.request.UpdateBuildingRequest;
import com.bookfair.backend.dto.building.response.BuildingResponse;
import com.bookfair.backend.dto.floor.mapper.FloorMapper;
import com.bookfair.backend.dto.floor.response.FloorResponse;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.VenueRepository;

import lombok.RequiredArgsConstructor;
import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final VenueRepository venueRepository;
    private final FloorRepository floorRepository;
    private final BuildingMapper buildingMapper;
    private final FloorMapper floorMapper;

    @Transactional
    public BuildingResponse createBuilding(CreateBuildingRequest request) {
        requireNonNull(request, "request cannot be null");
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));

        Building building = buildingMapper.toBuildingFromCreateBuildingRequest(request);
        building.setVenue(venue);
        building.setActive(true);
        building.setType(Building.BuildingType.valueOf(request.getType().toUpperCase()));

        Building saved = buildingRepository.save(building);
        return buildingMapper.toBuildingResponse(saved);
    }

    @Transactional(readOnly = true)
    public BuildingResponse getBuildingById(UUID id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        return buildingMapper.toBuildingResponse(building);
    }

    @Transactional
    public BuildingResponse updateBuilding(UUID id, UpdateBuildingRequest request) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));

        buildingMapper.updateBuildingFromBuildingRequest(request, building);
        building.setVenue(venue);
        building.setType(Building.BuildingType.valueOf(request.getType().toUpperCase()));

        Building saved = buildingRepository.save(building);
        return buildingMapper.toBuildingResponse(saved);
    }

    @Transactional
    public void deleteBuilding(UUID id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        building.setActive(false);
        buildingRepository.save(building);
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> getFloorsByBuilding(UUID buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND);
        }

        return floorRepository.findByBuildingIdOrderByLevelNumberAsc(buildingId).stream()
                .map(floorMapper::toFloorResponse)
                .collect(Collectors.toList());
    }
}
