package com.bookfair.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.stall.mapper.StallMapper;
import com.bookfair.backend.dto.stall.request.CreateStallRequest;
import com.bookfair.backend.dto.stall.request.UpdateStallRequest;
import com.bookfair.backend.dto.stall.response.StallResponse;
import com.bookfair.backend.event.stall.StallCreatedEvent;
import com.bookfair.backend.event.stall.StallDeactivatedEvent;
import com.bookfair.backend.event.stall.StallStatusChangedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StallService {

    private final StallRepository stallRepository;
    private final HallRepository hallRepository;
    private final EventStallRepository eventStallRepository;
    private final LayoutGenerationService layoutGenerationService;
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
        if (stallRequests == null || stallRequests.isEmpty()) {
            throw new IllegalArgumentException("Stall requests list must not be empty");
        }

        Set<String> seenNames = new HashSet<>();
        Map<UUID, Long> hallCounts = stallRequests.stream()
                .collect(Collectors.groupingBy(sr -> requireNonNull(sr.getHallId()), Collectors.counting()));

        for (Map.Entry<UUID, Long> entry : hallCounts.entrySet()) {
            UUID hallId = entry.getKey();
            long newCount = entry.getValue();
            Hall hall = hallRepository.findById(requireNonNull(hallId))
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Hall not found: " + hallId, ErrorCode.HALL_NOT_FOUND));
            if (hall.getMaxStalls() != null) {
                long currentCount = stallRepository.findByHallIdAndActiveTrue(hallId).size();
                if ((currentCount + newCount) > hall.getMaxStalls()) {
                    throw new IllegalStateException("Creating stalls exceeds Hall capacity limit of "
                            + hall.getMaxStalls() + " for hall " + hall.getName());
                }
            }
        }

        List<Stall> stalls = new ArrayList<>();
        for (CreateStallRequest req : stallRequests) {
            String nameKey = req.getHallId() + "-" + req.getName().toLowerCase();
            if (!seenNames.add(nameKey)) {
                throw new BusinessException("Duplicate stall name in batch request: " + req.getName(),
                        ErrorCode.BUSINESS_RULE_VIOLATION);
            }
            if (stallRepository.existsByHallIdAndName(req.getHallId(), req.getName())) {
                throw new DuplicateResourceException("Stall already exists with name: " + req.getName(),
                        ErrorCode.BUSINESS_RULE_VIOLATION);
            }
            Stall stall = stallMapper.toStallFromCreateStallRequest(req);
            layoutGenerationService.validateSpatialConstraints(stall.getHall(), stall.getLayout(), null);
            for (Stall alreadyAdded : stalls) {
                if (alreadyAdded.getHall().getId().equals(stall.getHall().getId()) && alreadyAdded.getLayout() != null
                        && stall.getLayout() != null) {
                    if (rectanglesOverlap(stall.getLayout().getXCoord(), stall.getLayout().getYCoord(),
                            stall.getLayout().getWidth(), stall.getLayout().getHeight(),
                            alreadyAdded.getLayout().getXCoord(), alreadyAdded.getLayout().getYCoord(),
                            alreadyAdded.getLayout().getWidth(), alreadyAdded.getLayout().getHeight())) {
                        throw new IllegalStateException("Requested stalls overlap with each other: " + stall.getName()
                                + " and " + alreadyAdded.getName());
                    }
                }
            }
            stalls.add(stall);
        }

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

        if (stallRequest.getActive() != null && !stallRequest.getActive() && "ACTIVE".equals(oldStatus)) {
            validateNoActiveBookingsForStall(id, stall.getName());
        }

        stallMapper.updateStallFromStallRequest(stallRequest, stall);

        if (stall.getLayout() != null) {
            layoutGenerationService.validateSpatialConstraints(stall.getHall(), stall.getLayout(), stall.getId());
        }

        Stall updatedStall = stallRepository.save(stall);

        if (stallRequest.getActive() != null && !(stallRequest.getActive() ? "ACTIVE" : "INACTIVE").equals(oldStatus)) {
            eventPublisher.publishEvent(new StallStatusChangedEvent(
                    requireNonNull(updatedStall.getId()),
                    requireNonNull(updatedStall.getName()),
                    oldStatus,
                    updatedStall.getActive() ? "ACTIVE" : "INACTIVE"));
            if (!updatedStall.getActive()) {
                eventPublisher.publishEvent(new StallDeactivatedEvent(updatedStall.getId()));
            }
        }

        return stallMapper.toStallResponse(updatedStall);
    }

    @Transactional
    public StallResponse updateStallStatus(UUID stallId, String newStatus) {
        Stall stall = stallRepository.findById(requireNonNull(stallId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Physical Stall not found", ErrorCode.STALL_NOT_FOUND));

        String oldStatus = stall.getActive() ? "ACTIVE" : "INACTIVE";
        boolean newActive = "ACTIVE".equalsIgnoreCase(newStatus);

        if (!newActive && "ACTIVE".equals(oldStatus)) {
            validateNoActiveBookingsForStall(stallId, stall.getName());
        }

        stall.setActive(newActive);

        Stall updatedStall = stallRepository.save(stall);

        eventPublisher.publishEvent(new StallStatusChangedEvent(
                requireNonNull(updatedStall.getId()),
                requireNonNull(updatedStall.getName()),
                oldStatus,
                requireNonNull(newStatus)));

        if (!newActive && "ACTIVE".equals(oldStatus)) {
            eventPublisher.publishEvent(new StallDeactivatedEvent(updatedStall.getId()));
        }

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
            validateNoActiveBookingsForStall(stall.getId(), stall.getName());
            stall.setActive(false);
            eventPublisher.publishEvent(new StallDeactivatedEvent(stall.getId()));
        }
        stallRepository.saveAll(requireNonNull(stalls));
    }

    private void validateNoActiveBookingsForStall(UUID stallId, String stallName) {
        List<EventStall> eventStalls = eventStallRepository.findByStallIdAndActiveTrue(stallId);
        for (EventStall es : eventStalls) {
            if (es.getStatus() == AvailabilityStatus.BOOKED || es.getStatus() == AvailabilityStatus.BLOCKED) {
                throw new BusinessException(
                        "Cannot deactivate stall " + stallName
                                + " because it is currently booked or blocked in an event.",
                        ErrorCode.BUSINESS_RULE_VIOLATION);
            }
        }
    }

    private boolean rectanglesOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
}
