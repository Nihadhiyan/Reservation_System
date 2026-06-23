package com.bookfair.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.ApiResponseDto;
import com.bookfair.backend.dto.event.request.CreateEventStallRequest;
import com.bookfair.backend.dto.event.response.EventStallResponse;
import com.bookfair.backend.service.EventStallService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/event-stalls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
public class EventStallController {

    private final EventStallService eventStallService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<EventStallResponse> assignStallToEvent(@Valid @RequestBody CreateEventStallRequest request) {
        EventStallResponse data = eventStallService.assignStallToEvent(request);
        return new ApiResponseDto<>(true, "Stall assigned to event successfully", data, LocalDateTime.now());
    }

    @GetMapping("/{id}")
    public ApiResponseDto<EventStallResponse> getEventStallById(@PathVariable UUID id) {
        EventStallResponse data = eventStallService.getEventStallById(id);
        return new ApiResponseDto<>(true, "Event stall fetched successfully", data, LocalDateTime.now());
    }

    @PutMapping("/{id}")
    public ApiResponseDto<EventStallResponse> updateEventStall(@PathVariable UUID id, @Valid @RequestBody CreateEventStallRequest request) {
        EventStallResponse data = eventStallService.updateEventStall(id, request);
        return new ApiResponseDto<>(true, "Event stall updated successfully", data, LocalDateTime.now());
    }

    @DeleteMapping("/{id}")
    public ApiResponseDto<Void> removeStallFromEvent(@PathVariable UUID id) {
        eventStallService.removeStallFromEvent(id);
        return new ApiResponseDto<>(true, "Stall removed from event successfully", null, LocalDateTime.now());
    }

    @GetMapping("/event/{eventId}")
    public ApiResponseDto<List<EventStallResponse>> getStallsForEvent(@PathVariable UUID eventId) {
        List<EventStallResponse> data = eventStallService.getStallsForEvent(eventId);
        return new ApiResponseDto<>(true, "Event stalls fetched successfully", data, LocalDateTime.now());
    }
}
