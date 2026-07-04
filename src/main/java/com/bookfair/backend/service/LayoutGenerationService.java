package com.bookfair.backend.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import com.bookfair.backend.event.cache.LayoutUpdatedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bookfair.backend.event.layout.HallDimensionsChangedEvent;

import com.bookfair.backend.dto.common.LayoutPositionDto;
import com.bookfair.backend.dto.common.Mapper.CommonMapper;
import com.bookfair.backend.dto.stall.mapper.StallMapper;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutMarker;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.LayoutMarkerRepository;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LayoutGenerationService {

    private final HallRepository hallRepository;
    private final StallRepository stallRepository;
    private final LayoutMarkerRepository layoutMarkerRepository;
    private final CommonMapper commonMapper;
    private final StallMapper stallMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<Stall> autoGenerateStallGrid(UUID hallId, int rows, int columns, int stallWidth, int stallLength,
            int aisleWidth, int startX, int startY) {
        requireNonNull(hallId, "hallId cannot be null");

        if (rows <= 0 || columns <= 0 || stallWidth <= 0 || stallLength <= 0 || aisleWidth < 0 || startX < 0 || startY < 0) {
            throw new IllegalArgumentException("Grid dimensions and coordinates must be positive values.");
        }

        if ((long) rows * columns > 1000) {
            throw new IllegalArgumentException("Grid generation exceeds maximum safety limit of 1000 stalls per request.");
        }

        Hall hall = hallRepository.findById(requireNonNull(hallId))
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND));

        List<Stall> existingStalls = stallRepository.findByHallIdAndActiveTrue(hallId);
        int currentCount = existingStalls.size();

        if (hall.getMaxStalls() != null && (currentCount + (rows * columns)) > hall.getMaxStalls()) {
            throw new IllegalStateException(String.format(
                "Generating %d stalls exceeds Hall capacity limit of %d (Current active stalls: %d)",
                (rows * columns), hall.getMaxStalls(), currentCount));
        }

        if (hall.getLayout() != null && hall.getLayout().getWidth() != null && hall.getLayout().getHeight() != null) {
            int totalGridWidth = startX + (columns * stallWidth) + ((columns - 1) * aisleWidth);
            int totalGridHeight = startY + (rows * stallLength) + ((rows - 1) * aisleWidth);
            if (totalGridWidth > hall.getLayout().getWidth() || totalGridHeight > hall.getLayout().getHeight()) {
                throw new IllegalStateException("Generated grid physical boundaries exceed parent Hall layout dimensions.");
            }
        }

        String prefix = hall.getName().replaceAll("[^A-Za-z0-9]", "");
        prefix = (prefix.length() >= 3 ? prefix.substring(0, 3) : prefix).toUpperCase();
        if (prefix.isEmpty()) prefix = "STL";

        int stallCounter = currentCount + 1;
        List<Stall> newStalls = new ArrayList<>();
        int currentY = startY;

        for (int r = 0; r < rows; r++) {
            int currentX = startX;
            for (int c = 0; c < columns; c++) {
                String name;
                do {
                    name = String.format("%s-%d", prefix, stallCounter++);
                } while (stallRepository.existsByHallIdAndName(hallId, name));

                Double sqFootage = (double) (stallWidth * stallLength);
                LayoutPosition layout = commonMapper.toLayoutPositionFromCoords(currentX, currentY, stallWidth, stallLength);

                validateSpatialConstraints(hall, layout, null);
                for (Stall newStall : newStalls) {
                    if (newStall.getLayout() != null && rectanglesOverlap(layout.getXCoord(), layout.getYCoord(), layout.getWidth(), layout.getHeight(),
                            newStall.getLayout().getXCoord(), newStall.getLayout().getYCoord(), newStall.getLayout().getWidth(), newStall.getLayout().getHeight())) {
                        throw new IllegalStateException("Generated stall spatial layout overlaps with another generated stall.");
                    }
                }

                Stall stall = stallMapper.toGeneratedStall(hall, name, sqFootage, layout);
                newStalls.add(stall);

                currentX += stallWidth + aisleWidth;
            }
            currentY += stallLength + aisleWidth;
        }

        log.info("Auto-generated {} stalls for Hall {}", newStalls.size(), hallId);
        List<Stall> savedStalls = stallRepository.saveAll(requireNonNull(newStalls));
        // Publish event to trigger AFTER_COMMIT cache eviction for both hall layout and admin dashboard metrics
        eventPublisher.publishEvent(new LayoutUpdatedEvent(hallId));
        return savedStalls;
    }

    @Transactional
    public Stall updateStallCoordinates(UUID stallId, LayoutPositionDto layoutPositionDto) {
        requireNonNull(layoutPositionDto, "layoutPositionDto cannot be null");
        Stall stall = stallRepository.findById(requireNonNull(stallId))
                .orElseThrow(() -> new ResourceNotFoundException("Stall not found", ErrorCode.STALL_NOT_FOUND));

        LayoutPosition newLayout = commonMapper.toLayoutPosition(layoutPositionDto);
        validateSpatialConstraints(stall.getHall(), newLayout, stallId);
        stall.setLayout(newLayout);

        log.info("Updated coordinates for stall {}", stallId);
        Stall savedStall = stallRepository.save(stall);
        // Publish event to trigger AFTER_COMMIT cache eviction
        eventPublisher.publishEvent(new LayoutUpdatedEvent(savedStall.getHall().getId()));
        return savedStall;
    }

    public void validateSpatialConstraints(Hall hall, LayoutPosition newLayout, UUID currentStallId) {
        if (newLayout == null || newLayout.getXCoord() == null || newLayout.getYCoord() == null
                || newLayout.getWidth() == null || newLayout.getHeight() == null) {
            throw new IllegalArgumentException("Layout position coordinates and dimensions must not be null");
        }
        if (newLayout.getXCoord() < 0 || newLayout.getYCoord() < 0 || newLayout.getWidth() <= 0 || newLayout.getHeight() <= 0) {
            throw new IllegalArgumentException("Layout coordinates must be non-negative and dimensions must be positive");
        }
        if (hall != null && hall.getLayout() != null && hall.getLayout().getWidth() != null && hall.getLayout().getHeight() != null) {
            if (newLayout.getXCoord() + newLayout.getWidth() > hall.getLayout().getWidth()
                    || newLayout.getYCoord() + newLayout.getHeight() > hall.getLayout().getHeight()) {
                throw new IllegalStateException("Stall layout exceeds parent Hall layout dimensions.");
            }
        }
        if (hall != null) {
            List<Stall> existingStalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
            for (Stall existing : existingStalls) {
                if (currentStallId != null && existing.getId().equals(currentStallId)) {
                    continue;
                }
                if (existing.getLayout() != null && existing.getLayout().getXCoord() != null && existing.getLayout().getYCoord() != null
                        && existing.getLayout().getWidth() != null && existing.getLayout().getHeight() != null) {
                    if (rectanglesOverlap(newLayout.getXCoord(), newLayout.getYCoord(), newLayout.getWidth(), newLayout.getHeight(),
                            existing.getLayout().getXCoord(), existing.getLayout().getYCoord(), existing.getLayout().getWidth(), existing.getLayout().getHeight())) {
                        throw new IllegalStateException("Stall spatial layout overlaps with existing stall: " + existing.getName());
                    }
                }
            }
            List<LayoutMarker> existingMarkers = layoutMarkerRepository.findByHallIdAndActiveTrue(hall.getId());
            for (LayoutMarker marker : existingMarkers) {
                if (marker.getLayout() != null && marker.getLayout().getXCoord() != null && marker.getLayout().getYCoord() != null
                        && marker.getLayout().getWidth() != null && marker.getLayout().getHeight() != null) {
                    if (rectanglesOverlap(newLayout.getXCoord(), newLayout.getYCoord(), newLayout.getWidth(), newLayout.getHeight(),
                            marker.getLayout().getXCoord(), marker.getLayout().getYCoord(), marker.getLayout().getWidth(), marker.getLayout().getHeight())) {
                        throw new IllegalStateException("Stall spatial layout overlaps with existing layout marker: " + marker.getLabel());
                    }
                }
            }
        }
    }

    private boolean rectanglesOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    // Cache physical stall layout lists by hall ID
    @Cacheable(value = "hallLayout", key = "#hallId")
    @Transactional(readOnly = true)
    public List<Stall> getHallLayout(UUID hallId) {
        if (!hallRepository.existsById(requireNonNull(hallId))) {
            throw new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND);
        }
        return stallRepository.findByHallIdAndActiveTrue(hallId);
    }

    @Async
    @EventListener
    @Transactional
    public void onHallDimensionsChanged(HallDimensionsChangedEvent event) {
        log.info("Verifying stall bounding box compliance after Hall {} dimension update to {}x{}",
                event.hallId(), event.newWidth(), event.newHeight());
        List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(event.hallId());
        List<Stall> outOfBoundsStalls = new ArrayList<>();
        for (Stall stall : stalls) {
            if (stall.getLayout() != null && stall.getLayout().getXCoord() != null && stall.getLayout().getYCoord() != null
                    && stall.getLayout().getWidth() != null && stall.getLayout().getHeight() != null) {
                int stallRight = stall.getLayout().getXCoord() + stall.getLayout().getWidth();
                int stallBottom = stall.getLayout().getYCoord() + stall.getLayout().getHeight();
                if (stallRight > event.newWidth() || stallBottom > event.newHeight()) {
                    log.warn("FLAGGED STALL OUT OF BOUNDS: Stall {} ({}) exceeds new Hall dimensions (Stall bottom-right: {},{} vs Hall: {},{})",
                            stall.getId(), stall.getName(), stallRight, stallBottom, event.newWidth(), event.newHeight());
                    outOfBoundsStalls.add(stall);
                }
            }
        }
        if (!outOfBoundsStalls.isEmpty()) {
            log.warn("Total flagged stalls out of bounds for Hall {}: {}", event.hallId(), outOfBoundsStalls.size());
        }
    }
}
