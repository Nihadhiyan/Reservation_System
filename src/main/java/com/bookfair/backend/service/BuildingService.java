package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.building.mapper.BuildingMapper;
import com.bookfair.backend.dto.building.request.CreateBuildingRequest;
import com.bookfair.backend.dto.building.request.UpdateBuildingRequest;
import com.bookfair.backend.dto.building.response.BuildingResponse;
import com.bookfair.backend.dto.floor.mapper.FloorMapper;
import com.bookfair.backend.dto.floor.response.FloorResponse;
import com.bookfair.backend.event.hierarchy.BuildingDeactivatedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.VenueRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final VenueRepository venueRepository;
    private final FloorRepository floorRepository;
    private final HallRepository hallRepository;
    private final StallRepository stallRepository;
    private final EventStallRepository eventStallRepository;
    private final BuildingMapper buildingMapper;
    private final FloorMapper floorMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BuildingResponse createBuilding(CreateBuildingRequest request) {
        requireNonNull(request, "request cannot be null");
        Venue venue = venueRepository.findById(requireNonNull(request.getVenueId()))
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
        Building building = buildingRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        return buildingMapper.toBuildingResponse(building);
    }

    @Transactional
    public BuildingResponse updateBuilding(UUID id, UpdateBuildingRequest request) {
        requireNonNull(request, "request cannot be null");
        Building building = buildingRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        Venue venue = venueRepository.findById(requireNonNull(request.getVenueId()))
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));

        if (!building.getVenue().getId().equals(venue.getId())) {
            List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelNumberAsc(building.getId());
            for (Floor floor : floors) {
                List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(floor.getId());
                for (Hall hall : halls) {
                    List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                    for (Stall stall : stalls) {
                        List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                        if (!esList.isEmpty()) {
                            throw new BusinessException("Cannot move Building across Venues because stalls are assigned to Events in the original Venue.", ErrorCode.BUSINESS_RULE_VIOLATION);
                        }
                    }
                }
            }
        }

        boolean oldActive = Boolean.TRUE.equals(building.getActive());
        if (request.getActive() != null && !request.getActive() && oldActive) {
            validateNoActiveBookingsForBuilding(building.getId(), building.getName());
        }

        buildingMapper.updateBuildingFromBuildingRequest(request, building);
        building.setVenue(venue);
        building.setType(Building.BuildingType.valueOf(request.getType().toUpperCase()));

        Building saved = buildingRepository.save(building);

        if (oldActive && !Boolean.TRUE.equals(saved.getActive())) {
            eventPublisher.publishEvent(new BuildingDeactivatedEvent(saved.getId()));
        }

        return buildingMapper.toBuildingResponse(saved);
    }

    @Transactional
    public void deleteBuilding(UUID id) {
        Building building = buildingRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        validateNoActiveBookingsForBuilding(building.getId(), building.getName());

        building.setActive(false);
        buildingRepository.save(building);
        eventPublisher.publishEvent(new BuildingDeactivatedEvent(building.getId()));
    }

    private void validateNoActiveBookingsForBuilding(UUID buildingId, String buildingName) {
        List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelNumberAsc(buildingId);
        for (Floor floor : floors) {
            List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(floor.getId());
            for (Hall hall : halls) {
                List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                for (Stall stall : stalls) {
                    List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                    for (EventStall es : esList) {
                        if (es.getStatus() == AvailabilityStatus.BOOKED || es.getStatus() == AvailabilityStatus.BLOCKED) {
                            throw new BusinessException("Cannot deactivate Building " + buildingName + " because stall " + stall.getName() + " is currently booked or blocked in an event.", ErrorCode.BUSINESS_RULE_VIOLATION);
                        }
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> getFloorsByBuilding(UUID buildingId) {
        if (!buildingRepository.existsById(requireNonNull(buildingId))) {
            throw new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND);
        }

        return floorRepository.findByBuildingIdOrderByLevelNumberAsc(buildingId).stream()
                .map(floorMapper::toFloorResponse)
                .collect(Collectors.toList());
    }
}
