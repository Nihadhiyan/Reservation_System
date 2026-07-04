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
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.HallRepository;
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
        stall.setLayout(newLayout);

        log.info("Updated coordinates for stall {}", stallId);
        Stall savedStall = stallRepository.save(stall);
        // Publish event to trigger AFTER_COMMIT cache eviction
        eventPublisher.publishEvent(new LayoutUpdatedEvent(savedStall.getHall().getId()));
        return savedStall;
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
