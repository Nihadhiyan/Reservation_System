package com.bookfair.backend.dto.event.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.bookfair.backend.model.Event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    private String name;

    @NotNull(message = "Venue ID is required")
    private UUID venueId;

    @NotNull(message = "Organizer ID is required")
    private UUID organizerId;

    private List<UUID> partnerIds;

    @NotNull(message = "Event type is required")
    private Event.EventType eventType;

    @NotBlank(message = "Event start date is required")
    private LocalDateTime startDateTime;

    @NotBlank(message = "Event end date is required")
    private LocalDateTime endDateTime;

    @NotBlank(message = "Event status is required")
    private Event.EventStatus status;
}
