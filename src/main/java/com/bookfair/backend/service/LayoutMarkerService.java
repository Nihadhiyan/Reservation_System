package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.common.LayoutMarkerDto;
import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.layout.request.CreateLayoutMarkerRequest;
import com.bookfair.backend.dto.layout.request.UpdateLayoutMarkerRequest;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutMarker;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.LayoutMarkerRepository;
import com.bookfair.backend.repository.VenueRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LayoutMarkerService {

    private final LayoutMarkerRepository layoutMarkerRepository;
    private final VenueRepository venueRepository;
    private final BuildingRepository buildingRepository;
    private final HallRepository hallRepository;
    private final CommonMapper commonMapper;

    @Transactional
    public LayoutMarkerDto createLayoutMarker(CreateLayoutMarkerRequest request) {
        requireNonNull(request, "request cannot be null");
        int parentCount = 0;
        if (request.getVenueId() != null)
            parentCount++;
        if (request.getBuildingId() != null)
            parentCount++;
        if (request.getHallId() != null)
            parentCount++;

        if (parentCount != 1) {
            throw new BusinessException("Exactly one parent (venue, building, or hall) must be specified.",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        LayoutMarker marker = new LayoutMarker();
        marker.setType(request.getType());
        marker.setLabel(request.getLabel());
        marker.setPrimaryMarker(request.getPrimaryMarker());
        marker.setActive(true);

        LayoutPosition layout = new LayoutPosition(
                request.getLayout().getXCoord(),
                request.getLayout().getYCoord(),
                request.getLayout().getWidth(),
                request.getLayout().getHeight());
        marker.setLayout(layout);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(requireNonNull(request.getVenueId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));
            marker.setVenue(venue);
        }

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(requireNonNull(request.getBuildingId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found", ErrorCode.VENUE_NOT_FOUND));
            marker.setBuilding(building);
        }

        if (request.getHallId() != null) {
            Hall hall = hallRepository.findById(requireNonNull(request.getHallId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND));
            marker.setHall(hall);
        }

        LayoutMarker saved = layoutMarkerRepository.save(marker);
        return commonMapper.toLayoutMarkerDto(saved);
    }

    @Transactional
    public LayoutMarkerDto updateLayoutMarker(UUID id, UpdateLayoutMarkerRequest request) {
        requireNonNull(request, "request cannot be null");
        LayoutMarker marker = layoutMarkerRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Layout marker not found", ErrorCode.HALL_NOT_FOUND));

        marker.setType(request.getType());
        marker.setLabel(request.getLabel());
        marker.setPrimaryMarker(request.getPrimaryMarker());
        marker.setActive(request.getActive());

        LayoutPosition layout = new LayoutPosition(
                request.getLayout().getXCoord(),
                request.getLayout().getYCoord(),
                request.getLayout().getWidth(),
                request.getLayout().getHeight());
        marker.setLayout(layout);

        LayoutMarker saved = layoutMarkerRepository.save(marker);
        return commonMapper.toLayoutMarkerDto(saved);
    }

    @Transactional
    public void deleteLayoutMarker(UUID id) {
        LayoutMarker marker = layoutMarkerRepository.findById(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Layout marker not found", ErrorCode.HALL_NOT_FOUND));

        marker.setActive(false);
        layoutMarkerRepository.save(marker);
    }

    @Transactional(readOnly = true)
    public List<LayoutMarkerDto> getMarkersByVenue(UUID venueId) {
        List<LayoutMarker> markers = layoutMarkerRepository.findByVenueIdAndActiveTrue(requireNonNull(venueId));
        return commonMapper.toLayoutMarkerDtos(markers);
    }

    @Transactional(readOnly = true)
    public List<LayoutMarkerDto> getMarkersByBuilding(UUID buildingId) {
        List<LayoutMarker> markers = layoutMarkerRepository.findByBuildingIdAndActiveTrue(requireNonNull(buildingId));
        return commonMapper.toLayoutMarkerDtos(markers);
    }

    @Transactional(readOnly = true)
    public List<LayoutMarkerDto> getMarkersByHall(UUID hallId) {
        List<LayoutMarker> markers = layoutMarkerRepository.findByHallIdAndActiveTrue(requireNonNull(hallId));
        return commonMapper.toLayoutMarkerDtos(markers);
    }
}
