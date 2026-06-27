package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.floor.mapper.FloorMapper;
import com.bookfair.backend.dto.floor.request.CreateFloorRequest;
import com.bookfair.backend.dto.floor.request.UpdateFloorRequest;
import com.bookfair.backend.dto.floor.response.FloorResponse;
import com.bookfair.backend.dto.hall.mapper.HallMapper;
import com.bookfair.backend.dto.hall.response.HallResponse;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FloorService {

    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final HallRepository hallRepository;
    private final FloorMapper floorMapper;
    private final HallMapper hallMapper;

    @Transactional
    public FloorResponse createFloor(CreateFloorRequest request) {
        requireNonNull(request, "request cannot be null");
        Building building = buildingRepository.findById(requireNonNull(request.getBuildingId()))
                .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));

        Floor floor = new Floor();
        floor.setLevelName(request.getLevelName());
        floor.setLevelNumber(request.getLevelNumber());
        floor.setBuilding(building);

        Floor saved = floorRepository.save(floor);
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

        floorMapper.UpdateFloorFromFloorRequest(request, floor);
        floor.setBuilding(building);

        Floor saved = floorRepository.save(floor);
        return floorMapper.toFloorResponse(saved);
    }

    @Transactional
    public void deleteFloor(UUID id) {
        Floor floor = floorRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found", ErrorCode.VENUE_NOT_FOUND));

        floorRepository.delete(requireNonNull(floor));
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
