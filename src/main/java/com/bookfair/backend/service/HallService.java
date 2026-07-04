package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.bookfair.backend.event.hierarchy.HallDeactivatedEvent;
import com.bookfair.backend.event.cache.HallUpdatedEvent;
import com.bookfair.backend.event.layout.HallDimensionsChangedEvent;
import java.util.Objects;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HallService {

        private final HallRepository hallRepository;
        private final FloorRepository floorRepository;
        private final StallRepository stallRepository;
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

                hall.setName(request.getName());
                hall.setSpaceCategory(request.getSpaceCategory());
                hall.setHallType(request.getHallType());
                hall.setBlueprintImageUrl(request.getBlueprintImageUrl());
                hall.setSquareFootage(request.getSquareFootage());
                hall.setMaxStalls(request.getMaxStalls());
                hall.setWifiAvailable(request.getWifiAvailable());
                hall.setAirConditioned(request.getAirConditioned());
                hall.setActive(request.getActive());

                LayoutPosition layout = commonMapper.toLayoutPosition(request.getLayout());
                hall.setLayout(layout);
                hall.setFloor(floor);

                Hall saved = hallRepository.save(hall);
                eventPublisher.publishEvent(new HallUpdatedEvent(saved.getId()));

                Integer newWidth = (saved.getLayout() != null) ? saved.getLayout().getWidth() : null;
                Integer newHeight = (saved.getLayout() != null) ? saved.getLayout().getHeight() : null;
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

                hall.setActive(false);
                hallRepository.save(hall);
                eventPublisher.publishEvent(new HallDeactivatedEvent(hall.getId()));
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
