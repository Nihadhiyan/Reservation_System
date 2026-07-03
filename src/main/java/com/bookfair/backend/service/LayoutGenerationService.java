package com.bookfair.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public List<Stall> autoGenerateStallGrid(UUID hallId, int rows, int columns, int stallWidth, int stallLength,
            int aisleWidth, int startX, int startY) {
        requireNonNull(hallId, "hallId cannot be null");
        Hall hall = hallRepository.findById(requireNonNull(hallId))
                .orElseThrow(() -> new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND));

        List<Stall> newStalls = new ArrayList<>();
        int currentY = startY;

        int stallCounter = 1;

        for (int r = 0; r < rows; r++) {
            int currentX = startX;
            for (int c = 0; c < columns; c++) {
                String name = String.format("%s-%d",
                        hall.getName().substring(0, Math.min(3, hall.getName().length())).toUpperCase(),
                        stallCounter++);
                Double sqFootage = (double) (stallWidth * stallLength);
                LayoutPosition layout = commonMapper.toLayoutPositionFromCoords(currentX, currentY, stallWidth, stallLength);

                Stall stall = stallMapper.toGeneratedStall(hall, name, sqFootage, layout);

                newStalls.add(stall);

                currentX += stallWidth + aisleWidth;
            }
            currentY += stallLength + aisleWidth;
        }

        log.info("Auto-generated {} stalls for Hall {}", newStalls.size(), hallId);
        return stallRepository.saveAll(requireNonNull(newStalls));
    }

    @Transactional
    public Stall updateStallCoordinates(UUID stallId, LayoutPositionDto layoutPositionDto) {
        requireNonNull(layoutPositionDto, "layoutPositionDto cannot be null");
        Stall stall = stallRepository.findById(requireNonNull(stallId))
                .orElseThrow(() -> new ResourceNotFoundException("Stall not found", ErrorCode.STALL_NOT_FOUND));

        LayoutPosition newLayout = commonMapper.toLayoutPosition(layoutPositionDto);
        stall.setLayout(newLayout);

        log.info("Updated coordinates for stall {}", stallId);
        return stallRepository.save(stall);
    }

    @Transactional(readOnly = true)
    public List<Stall> getHallLayout(UUID hallId) {
        if (!hallRepository.existsById(requireNonNull(hallId))) {
            throw new ResourceNotFoundException("Hall not found", ErrorCode.HALL_NOT_FOUND);
        }
        return stallRepository.findByHallIdAndActiveTrue(hallId);
    }
}
