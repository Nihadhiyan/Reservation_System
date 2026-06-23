package com.bookfair.backend.service;

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
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.StallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventStallService {

    private final EventStallRepository eventStallRepository;
    private final EventRepository eventRepository;
    private final StallRepository stallRepository;
    private final EventMapper eventMapper;

    @Transactional
    public EventStallResponse assignStallToEvent(CreateEventStallRequest request) {
        Event event = eventRepository.findByIdAndActiveTrue(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        Stall stall = stallRepository.findByIdAndActiveTrue(request.getStallId())
                .orElseThrow(() -> new ResourceNotFoundException("Stall not found", ErrorCode.STALL_NOT_FOUND));

        eventStallRepository.findByEventIdAndStallId(request.getEventId(), request.getStallId())
                .ifPresent(existing -> {
                    throw new BusinessException("Stall is already assigned to this event.", ErrorCode.BUSINESS_RULE_VIOLATION);
                });

        EventStall eventStall = new EventStall();
        eventStall.setEvent(event);
        eventStall.setStall(stall);
        eventStall.setBasePrice(request.getBasePrice());
        eventStall.setManualOverridePrice(request.getManualOverridePrice());
        eventStall.setStatus(EventStall.AvailabilityStatus.valueOf(request.getStatus().toUpperCase()));

        EventStall saved = eventStallRepository.save(eventStall);
        return eventMapper.toEventStallResponse(saved);
    }

    @Transactional(readOnly = true)
    public EventStallResponse getEventStallById(UUID id) {
        EventStall eventStall = eventStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found", ErrorCode.EVENT_NOT_FOUND));

        return eventMapper.toEventStallResponse(eventStall);
    }

    @Transactional
    public EventStallResponse updateEventStall(UUID id, CreateEventStallRequest request) {
        EventStall eventStall = eventStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found", ErrorCode.EVENT_NOT_FOUND));

        eventStall.setBasePrice(request.getBasePrice());
        eventStall.setManualOverridePrice(request.getManualOverridePrice());
        eventStall.setStatus(EventStall.AvailabilityStatus.valueOf(request.getStatus().toUpperCase()));

        EventStall saved = eventStallRepository.save(eventStall);
        return eventMapper.toEventStallResponse(saved);
    }

    @Transactional
    public void removeStallFromEvent(UUID id) {
        EventStall eventStall = eventStallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event stall not found", ErrorCode.EVENT_NOT_FOUND));

        if (eventStall.getStatus() == EventStall.AvailabilityStatus.BOOKED) {
            throw new BusinessException("Cannot remove a booked stall from the event.", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        eventStallRepository.delete(eventStall);
    }

    @Transactional(readOnly = true)
    public List<EventStallResponse> getStallsForEvent(UUID eventId) {
        if (!eventRepository.findByIdAndActiveTrue(eventId).isPresent()) {
            throw new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND);
        }

        return eventStallRepository.findAllByEventIdWithStallData(eventId).stream()
                .map(eventMapper::toEventStallResponse)
                .collect(Collectors.toList());
    }
}
