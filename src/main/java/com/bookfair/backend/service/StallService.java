package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.stall.mapper.StallMapper;
import com.bookfair.backend.dto.stall.request.CreateStallRequest;
import com.bookfair.backend.dto.stall.request.UpdateStallRequest;
import com.bookfair.backend.dto.stall.response.StallResponse;
import com.bookfair.backend.event.stall.StallCreatedEvent;
import com.bookfair.backend.event.stall.StallStatusChangedEvent;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StallService {

    private final StallRepository stallRepository;
    private final StallMapper stallMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<StallResponse> getAllStallsForHall(UUID hallId) {
        requireNonNull(hallId, "hallId cannot be null");
        return stallRepository.findByHallIdAndActiveTrue(hallId).stream()
                .map(stallMapper::toStallResponse).toList();
    }

    @Transactional(readOnly = true)
    public StallResponse getStallById(UUID id) {
        Stall stall = stallRepository.findById(requireNonNull(id))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Physical Stall not found", ErrorCode.STALL_NOT_FOUND));

        return stallMapper.toStallResponse(stall);
    }

    @Transactional
    public List<StallResponse> createStalls(List<CreateStallRequest> stallRequests, String currentUser) {
        List<Stall> stalls = requireNonNull(stallRequests).stream().map(stallMapper::toStallFromCreateStallRequest)
                .toList();

        List<Stall> savedStalls = stallRepository.saveAll(requireNonNull(stalls));

        savedStalls.forEach(savedStall -> {
            eventPublisher.publishEvent(new StallCreatedEvent(
                    requireNonNull(savedStall.getId()),
                    requireNonNull(savedStall.getName()),
                    requireNonNull(savedStall.getHall().getId()),
                    requireNonNull(currentUser)));
            log.info("Stall {} created successfully", savedStall.getName());
        });

        return savedStalls.stream().map(stallMapper::toStallResponse).toList();
    }

    @Transactional
    public StallResponse updateStall(UUID id, UpdateStallRequest stallRequest) {
        Stall stall = stallRepository.findById(requireNonNull(id))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Physical Stall not found", ErrorCode.STALL_NOT_FOUND));

        String oldStatus = stall.getActive() ? "ACTIVE" : "INACTIVE";

        stallMapper.updateStallFromStallRequest(stallRequest, stall);

        Stall updatedStall = stallRepository.save(stall);

        if (stallRequest.getActive() != null && !(stallRequest.getActive() ? "ACTIVE" : "INACTIVE").equals(oldStatus)) {
            eventPublisher.publishEvent(new StallStatusChangedEvent(
                    requireNonNull(updatedStall.getId()),
                    requireNonNull(updatedStall.getName()),
                    oldStatus,
                    updatedStall.getActive() ? "ACTIVE" : "INACTIVE"));
        }

        return stallMapper.toStallResponse(updatedStall);
    }

    @Transactional
    public StallResponse updateStallStatus(UUID stallId, String newStatus) {
        Stall stall = stallRepository.findById(requireNonNull(stallId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Physical Stall not found", ErrorCode.STALL_NOT_FOUND));

        String oldStatus = stall.getActive() ? "ACTIVE" : "INACTIVE";
        stall.setActive("ACTIVE".equalsIgnoreCase(newStatus));

        Stall updatedStall = stallRepository.save(stall);

        eventPublisher.publishEvent(new StallStatusChangedEvent(
                requireNonNull(updatedStall.getId()),
                requireNonNull(updatedStall.getName()),
                oldStatus,
                requireNonNull(newStatus)));

        return stallMapper.toStallResponse(updatedStall);
    }

    @Transactional(readOnly = true)
    public List<StallResponse> getAvailableStalls() {
        return stallRepository.findAllByActiveTrue().stream()
                .map(stallMapper::toStallResponse).toList();
    }

    @Transactional
    public void deactivateStall(List<UUID> ids) {
        List<Stall> stalls = stallRepository.findAllByIdInAndActiveTrue(requireNonNull(ids));
        for (Stall stall : stalls) {
            stall.setActive(false);
        }
        stallRepository.saveAll(requireNonNull(stalls));
    }
}
