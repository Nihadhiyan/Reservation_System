package com.bookfair.backend.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import com.bookfair.backend.event.cache.EventStallUpdatedEvent;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.event.mapper.EventMapper;
import com.bookfair.backend.dto.event.request.CreateEventStallRequest;
import com.bookfair.backend.dto.event.response.EventStallResponse;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventStallService {

        private final EventStallRepository eventStallRepository;
        private final EventRepository eventRepository;
        private final StallRepository stallRepository;
        private final HallRepository hallRepository;
        private final EventMapper eventMapper;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public EventStallResponse assignStallToEvent(CreateEventStallRequest request) {
                requireNonNull(request, "request cannot be null");
                Event event = eventRepository.findByIdAndActiveTrue(request.getEventId())
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                Stall stall = stallRepository.findByIdAndActiveTrue(request.getStallId())
                                .orElseThrow(() -> new ResourceNotFoundException("Stall not found",
                                                ErrorCode.STALL_NOT_FOUND));

                UUID stallVenueId = stall.getHall().getFloor().getBuilding().getVenue().getId();
                if (!stallVenueId.equals(event.getVenue().getId())) {
                        throw new BusinessException(
                                        "Relational integrity violation: Stall does not belong to the Event's Venue.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                java.util.Optional<EventStall> existingOpt = eventStallRepository
                                .findByEventIdAndStallId(request.getEventId(), request.getStallId());
                if (existingOpt.isPresent()) {
                        EventStall existing = existingOpt.get();
                        if (Boolean.TRUE.equals(existing.getActive())) {
                                throw new BusinessException("Stall is already assigned to this event.",
                                                ErrorCode.BUSINESS_RULE_VIOLATION);
                        } else {
                                existing.setActive(true);
                                existing.setStatus(EventStall.AvailabilityStatus.AVAILABLE);
                                EventStall saved = eventStallRepository.save(existing);
                                eventPublisher.publishEvent(new EventStallUpdatedEvent(request.getEventId()));
                                return eventMapper.toEventStallResponse(saved);
                        }
                }

                EventStall eventStall = eventMapper.toEventStall(request, event, stall);

                EventStall saved = eventStallRepository.save(requireNonNull(eventStall));
                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventStallUpdatedEvent(request.getEventId()));
                return eventMapper.toEventStallResponse(saved);
        }

        @Transactional(readOnly = true)
        public EventStallResponse getEventStallById(UUID id) {
                EventStall eventStall = eventStallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                return eventMapper.toEventStallResponse(eventStall);
        }

        @Transactional
        public EventStallResponse updateEventStall(UUID id, CreateEventStallRequest request) {
                requireNonNull(request, "request cannot be null");
                EventStall eventStall = eventStallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                EventStall.AvailabilityStatus newStatus = EventStall.AvailabilityStatus
                                .valueOf(request.getStatus().toUpperCase());
                if (newStatus == EventStall.AvailabilityStatus.AVAILABLE &&
                                (eventStall.getStatus() == EventStall.AvailabilityStatus.BOOKED
                                                || eventStall.getStatus() == EventStall.AvailabilityStatus.BLOCKED)) {
                        throw new BusinessException(
                                        "Cannot manually change status to AVAILABLE while stall is currently booked or blocked by an active reservation.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                eventStall.setBasePrice(request.getBasePrice());
                eventStall.setManualOverridePrice(request.getManualOverridePrice());
                eventStall.setStatus(newStatus);

                EventStall saved = eventStallRepository.save(eventStall);
                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventStallUpdatedEvent(saved.getEvent().getId()));
                return eventMapper.toEventStallResponse(saved);
        }

        @Transactional
        public void removeStallFromEvent(UUID id) {
                EventStall eventStall = eventStallRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                if (eventStall.getStatus() == EventStall.AvailabilityStatus.BOOKED
                                || eventStall.getStatus() == EventStall.AvailabilityStatus.BLOCKED) {
                        throw new BusinessException("Cannot remove a booked or blocked stall from the event.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                UUID eventId = eventStall.getEvent().getId();
                eventStallRepository.delete(eventStall);
                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventStallUpdatedEvent(eventId));
        }

        // Cache stalls assigned to an event by event ID
        @Cacheable(value = "eventStalls", key = "#eventId")
        @Transactional(readOnly = true)
        public List<EventStallResponse> getStallsForEvent(UUID eventId) {
                if (!eventRepository.findByIdAndActiveTrue(requireNonNull(eventId)).isPresent()) {
                        throw new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND);
                }

                return eventStallRepository.findAllByEventIdWithStallData(eventId).stream()
                                .filter(es -> es != null && Boolean.TRUE.equals(es.getActive())
                                                && Boolean.TRUE.equals(es.getStall().getActive()))
                                .map(eventMapper::toEventStallResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public List<EventStallResponse> copyAllStallsFromHall(UUID eventId, UUID hallId) {
                Event event = eventRepository.findByIdAndActiveTrue(requireNonNull(eventId))
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                Hall hall = hallRepository.findById(requireNonNull(hallId))
                                .orElseThrow(() -> new ResourceNotFoundException("Hall not found",
                                                ErrorCode.HALL_NOT_FOUND));

                UUID hallVenueId = hall.getFloor().getBuilding().getVenue().getId();
                if (!hallVenueId.equals(event.getVenue().getId())) {
                        throw new BusinessException(
                                        "Relational integrity violation: Hall does not belong to the Event's Venue.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hallId);
                List<EventStall> existingEventStalls = eventStallRepository.findByEventId(eventId);
                java.util.Map<UUID, EventStall> existingByStallId = existingEventStalls.stream()
                                .collect(Collectors.toMap(es -> es.getStall().getId(), es -> es));

                List<EventStall> stallsToSave = new java.util.ArrayList<>();
                for (Stall stall : stalls) {
                        EventStall existing = existingByStallId.get(stall.getId());
                        if (existing != null) {
                                if (Boolean.FALSE.equals(existing.getActive())) {
                                        existing.setActive(true);
                                        existing.setStatus(EventStall.AvailabilityStatus.AVAILABLE);
                                        stallsToSave.add(existing);
                                }
                        } else {
                                stallsToSave.add(eventMapper.toCopiedEventStall(event, stall));
                        }
                }

                eventStallRepository.saveAll(requireNonNull(stallsToSave));
                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventStallUpdatedEvent(eventId));
                return eventStallRepository.findByEventIdAndActiveTrue(eventId).stream()
                                .map(eventMapper::toEventStallResponse)
                                .collect(Collectors.toList());
        }
}
