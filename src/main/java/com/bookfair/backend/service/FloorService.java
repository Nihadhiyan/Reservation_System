package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.floor.mapper.FloorMapper;
import com.bookfair.backend.dto.floor.request.CreateFloorRequest;
import com.bookfair.backend.dto.floor.request.UpdateFloorRequest;
import com.bookfair.backend.dto.floor.response.FloorResponse;
import com.bookfair.backend.dto.hall.mapper.HallMapper;
import com.bookfair.backend.dto.hall.response.HallResponse;
import com.bookfair.backend.event.hierarchy.FloorDeactivatedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FloorService {

    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final HallRepository hallRepository;
    private final StallRepository stallRepository;
    private final EventStallRepository eventStallRepository;
    private final FloorMapper floorMapper;
    private final HallMapper hallMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FloorResponse createFloor(CreateFloorRequest request) {
        requireNonNull(request, "request cannot be null");
        Building building = buildingRepository.findById(requireNonNull(request.getBuildingId()))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        Floor floor = floorMapper.toFloor(request, building);
        floor.setActive(true);

        Floor saved = floorRepository.save(requireNonNull(floor));
        return floorMapper.toFloorResponse(saved);
    }

    @Transactional(readOnly = true)
    public FloorResponse getFloorById(UUID id) {
        Floor floor = floorRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found", ErrorCode.VENUE_NOT_FOUND));

        return floorMapper.toFloorResponse(floor);
    }

    @Transactional
    public FloorResponse updateFloor(UUID id, UpdateFloorRequest request) {
        requireNonNull(request, "request cannot be null");
        Floor floor = floorRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found", ErrorCode.VENUE_NOT_FOUND));

        Building building = buildingRepository.findById(requireNonNull(request.getBuildingId()))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        if (!floor.getBuilding().getId().equals(building.getId())) {
            UUID oldVenueId = floor.getBuilding().getVenue().getId();
            UUID newVenueId = building.getVenue().getId();
            if (!oldVenueId.equals(newVenueId)) {
                List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(floor.getId());
                for (Hall hall : halls) {
                    List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                    for (Stall stall : stalls) {
                        List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                        if (!esList.isEmpty()) {
                            throw new BusinessException("Cannot move Floor across Venues because stalls are assigned to Events in the original Venue.", ErrorCode.BUSINESS_RULE_VIOLATION);
                        }
                    }
                }
            }
        }

        floorMapper.UpdateFloorFromFloorRequest(request, floor);
        floor.setBuilding(building);

        Floor saved = floorRepository.save(floor);

        return floorMapper.toFloorResponse(saved);
    }

    @Transactional
    public void deleteFloor(UUID id) {
        Floor floor = floorRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found", ErrorCode.VENUE_NOT_FOUND));

        validateNoActiveBookingsForFloor(floor.getId(), floor.getLevelName());

        floor.setActive(false);
        floorRepository.save(floor);
        eventPublisher.publishEvent(new FloorDeactivatedEvent(floor.getId()));
    }

    private void validateNoActiveBookingsForFloor(UUID floorId, String floorName) {
        List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(floorId);
        for (Hall hall : halls) {
            List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
            for (Stall stall : stalls) {
                List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                for (EventStall es : esList) {
                    if (es.getStatus() == AvailabilityStatus.BOOKED || es.getStatus() == AvailabilityStatus.BLOCKED) {
                        throw new BusinessException("Cannot deactivate Floor " + floorName + " because stall " + stall.getName() + " is currently booked or blocked in an event.", ErrorCode.BUSINESS_RULE_VIOLATION);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<HallResponse> getHallsByFloor(UUID floorId) {
        if (!floorRepository.existsById(requireNonNull(floorId))) {
            throw new ResourceNotFoundException("Floor not found", ErrorCode.VENUE_NOT_FOUND);
        }

        return hallRepository.findByFloorIdAndActiveTrue(floorId).stream()
                .map(hallMapper::toHallResponse)
                .collect(Collectors.toList());
    }
}
