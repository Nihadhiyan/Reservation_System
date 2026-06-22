package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.event.mapper.EventMapper;
import com.bookfair.backend.dto.event.response.EventResponse;
import com.bookfair.backend.dto.event.response.EventStallResponse;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.Event.EventStatus;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventStallRepository eventStallRepository;
    private final EventMapper eventMapper;

    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findByStatusAndActiveTrue(EventStatus.UPCOMING).stream()
            .map(event -> {
                return eventMapper.toEventResponse(event);
            })
            .toList();
    }

    public List<EventStallResponse> getStallsForEvent(UUID eventId) {
        Event event = eventRepository.findByIdAndActiveTrue(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        return eventStallRepository.findByEvent(event).stream().map(eventStall -> {
            return eventMapper.toEventStallResponse(eventStall);
        })
        .toList();
    }
}
