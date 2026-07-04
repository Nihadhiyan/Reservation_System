package com.bookfair.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.hall.mapper.HallMapper;
import com.bookfair.backend.dto.hall.request.CreateHallRequest;
import com.bookfair.backend.dto.hall.request.UpdateHallRequest;
import com.bookfair.backend.dto.hall.response.HallLayoutResponse;
import com.bookfair.backend.dto.hall.response.HallResponse;
import com.bookfair.backend.dto.layout.request.GenerateStallGridRequest;
import com.bookfair.backend.dto.stall.mapper.StallMapper;
import com.bookfair.backend.dto.stall.response.StallResponse;
import com.bookfair.backend.event.cache.HallUpdatedEvent;
import com.bookfair.backend.event.hierarchy.HallDeactivatedEvent;
import com.bookfair.backend.event.layout.HallDimensionsChangedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutMarker;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.LayoutMarkerRepository;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HallService {

        private final HallRepository hallRepository;
        private final FloorRepository floorRepository;
        private final StallRepository stallRepository;
        private final EventStallRepository eventStallRepository;
        private final LayoutMarkerRepository layoutMarkerRepository;
        private final HallMapper hallMapper;
        private final StallMapper stallMapper;
        private final LayoutGenerationService layoutGenerationService;
        private final CommonMapper commonMapper;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public HallResponse createHall(CreateHallRequest request) {
                requireNonNull(request, "request cannot be null");
                Floor floor = floorRepository.findById(requireNonNull(request.getFloorId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Floor not found",
                                                ErrorCode.VENUE_NOT_FOUND));

                Hall hall = hallMapper.toHall(request, floor);

                Hall saved = hallRepository.save(requireNonNull(hall));
                return hallMapper.toHallResponse(saved);
        }

        @Transactional(readOnly = true)
        public HallResponse getHallById(UUID id) {
                Hall hall = hallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Hall not found",
                                                ErrorCode.HALL_NOT_FOUND));

                return hallMapper.toHallResponse(hall);
        }

        @Transactional(readOnly = true)
        public HallLayoutResponse getHallLayout(UUID id) {
                Hall hall = hallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Hall not found",
                                                ErrorCode.HALL_NOT_FOUND));

                return hallMapper.toHallLayoutResponse(hall);
        }

        @Transactional
        public HallResponse updateHall(UUID id, UpdateHallRequest request) {
                requireNonNull(request, "request cannot be null");
                Hall hall = hallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Hall not found",
                                                ErrorCode.HALL_NOT_FOUND));

                Integer oldWidth = (hall.getLayout() != null) ? hall.getLayout().getWidth() : null;
                Integer oldHeight = (hall.getLayout() != null) ? hall.getLayout().getHeight() : null;

                Floor floor = floorRepository.findById(requireNonNull(request.getFloorId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Floor not found",
                                                ErrorCode.VENUE_NOT_FOUND));

                if (!hall.getFloor().getId().equals(floor.getId())) {
                        UUID oldVenueId = hall.getFloor().getBuilding().getVenue().getId();
                        UUID newVenueId = floor.getBuilding().getVenue().getId();
                        if (!oldVenueId.equals(newVenueId)) {
                                List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                                for (Stall stall : stalls) {
                                        List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                                        if (!esList.isEmpty()) {
                                                throw new BusinessException("Cannot move Hall across Venues because stalls are assigned to Events in the original Venue.",
                                                                ErrorCode.BUSINESS_RULE_VIOLATION);
                                        }
                                }
                        }
                }

                if (request.getActive() != null && !request.getActive() && Boolean.TRUE.equals(hall.getActive())) {
                        validateNoActiveBookingsForHall(hall.getId(), hall.getName());
                }

                LayoutPosition layout = commonMapper.toLayoutPosition(request.getLayout());
                Integer newWidth = (layout != null) ? layout.getWidth() : null;
                Integer newHeight = (layout != null) ? layout.getHeight() : null;

                if (newWidth != null && newHeight != null && (!Objects.equals(oldWidth, newWidth) || !Objects.equals(oldHeight, newHeight))) {
                        List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                        for (Stall stall : stalls) {
                                if (stall.getLayout() != null && stall.getLayout().getXCoord() != null && stall.getLayout().getYCoord() != null
                                                && stall.getLayout().getWidth() != null && stall.getLayout().getHeight() != null) {
                                        if (stall.getLayout().getXCoord() + stall.getLayout().getWidth() > newWidth
                                                        || stall.getLayout().getYCoord() + stall.getLayout().getHeight() > newHeight) {
                                                throw new IllegalStateException("Cannot resize Hall: Stall " + stall.getName() + " would exceed new dimensions.");
                                        }
                                }
                        }
                        List<LayoutMarker> markers = layoutMarkerRepository.findByHallIdAndActiveTrue(hall.getId());
                        for (LayoutMarker marker : markers) {
                                if (marker.getLayout() != null && marker.getLayout().getXCoord() != null && marker.getLayout().getYCoord() != null
                                                && marker.getLayout().getWidth() != null && marker.getLayout().getHeight() != null) {
                                        if (marker.getLayout().getXCoord() + marker.getLayout().getWidth() > newWidth
                                                        || marker.getLayout().getYCoord() + marker.getLayout().getHeight() > newHeight) {
                                                throw new IllegalStateException("Cannot resize Hall: LayoutMarker " + marker.getLabel() + " would exceed new dimensions.");
                                        }
                                }
                        }
                }

                hall.setName(request.getName());
                hall.setSpaceCategory(request.getSpaceCategory());
                hall.setHallType(request.getHallType());
                hall.setBlueprintImageUrl(request.getBlueprintImageUrl());
                hall.setSquareFootage(request.getSquareFootage());
                hall.setMaxStalls(request.getMaxStalls());
                hall.setWifiAvailable(request.getWifiAvailable());
                hall.setAirConditioned(request.getAirConditioned());
                hall.setActive(request.getActive());
                hall.setLayout(layout);
                hall.setFloor(floor);

                Hall saved = hallRepository.save(hall);
                eventPublisher.publishEvent(new HallUpdatedEvent(saved.getId()));

                if (newWidth != null && newHeight != null && (!Objects.equals(oldWidth, newWidth) || !Objects.equals(oldHeight, newHeight))) {
                        eventPublisher.publishEvent(new HallDimensionsChangedEvent(saved.getId(), newWidth, newHeight));
                }

                return hallMapper.toHallResponse(saved);
        }

        @Transactional
        public void deleteHall(UUID id) {
                Hall hall = hallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Hall not found",
                                                ErrorCode.HALL_NOT_FOUND));

                validateNoActiveBookingsForHall(hall.getId(), hall.getName());

                hall.setActive(false);
                hallRepository.save(hall);
                eventPublisher.publishEvent(new HallDeactivatedEvent(hall.getId()));
        }

        private void validateNoActiveBookingsForHall(UUID hallId, String hallName) {
                List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hallId);
                for (Stall stall : stalls) {
                        List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                        for (EventStall es : esList) {
                                if (es.getStatus() == AvailabilityStatus.BOOKED || es.getStatus() == AvailabilityStatus.BLOCKED) {
                                        throw new BusinessException("Cannot deactivate Hall " + hallName + " because stall " + stall.getName() + " is currently booked or blocked in an event.",
                                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                                }
                        }
                }
        }

        @Transactional(readOnly = true)
        public List<StallResponse> getStallsByHall(UUID hallId) {
                if (!hallRepository.existsById(requireNonNull(hallId))) {
                        throw new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND);
                }

                return stallRepository.findByHallIdAndActiveTrue(hallId).stream()
                                .map(stallMapper::toStallResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public List<StallResponse> generateStallGrid(UUID hallId, GenerateStallGridRequest request) {
                List<Stall> generatedStalls = layoutGenerationService.autoGenerateStallGrid(
                                hallId,
                                request.getRows(),
                                request.getColumns(),
                                request.getStallWidth(),
                                request.getStallLength(),
                                request.getAisleWidth(),
                                request.getStartX(),
                                request.getStartY());

                return generatedStalls.stream()
                                .map(stallMapper::toStallResponse)
                                .collect(Collectors.toList());
        }
}
