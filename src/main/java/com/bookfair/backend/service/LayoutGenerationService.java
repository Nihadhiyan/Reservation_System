package com.bookfair.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.LayoutPosition;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LayoutGenerationService {
    private final StallRepository stallRepository;
    private final HallRepository hallRepository;

    @Transactional
    public List<Stall> autoGenerateStallGrid (
        UUID hallId, 
        int rows, 
        int columns, 
        int stallWidth, 
        int stallLength,
        int aisleWidth, 
        int startX, 
        int startY
    ) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("Hall not found with ID: " + hallId));

        List<Stall> generatedStalls = new ArrayList<>();

        int currentY = startY;
        int stallCounter = 1;

        for (int r = 0; r < rows; r++) {
            int currentX = startX;

            for (int c = 0; c < columns; c++) {
                Stall stall = new Stall();
                stall.setName(hall.getName() + " - Stall " + stallCounter++);
                stall.setHall(hall);
                stall.setStallType(Stall.StallType.STANDARD);
                stall.setSquareFootage((double) (stallWidth * stallLength));
                stall.setActive(true);

                LayoutPosition layoutPosition = new LayoutPosition(currentX, currentY, stallWidth, stallLength);
                stall.setLayout(layoutPosition);

                generatedStalls.add(stall);

                currentX += stallWidth + aisleWidth;
            }

            currentY += stallLength + aisleWidth;
        }

        int currentCount = hall.getCurrentStallCount() != null ? hall.getCurrentStallCount() : 0;

        hall.setCurrentStallCount(currentCount + generatedStalls.size());
        hallRepository.save(hall);

        return stallRepository.saveAll(generatedStalls);
    }
}
