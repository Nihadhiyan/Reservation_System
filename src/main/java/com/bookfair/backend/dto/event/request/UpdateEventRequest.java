package com.bookfair.backend.dto.event.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.bookfair.backend.model.Event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    @NotNull(message = "Event name is required")
    private String name;

    @NotNull(message = "Venue ID is required")
    private UUID venueId;

    @NotNull(message = "Organizer ID is required")
    private UUID organizerId;

    private List<UUID> partnerIds;

    @NotNull(message = "Event type is required")
    private Event.EventType eventType;

    @NotNull(message = "Event start date is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "Event end date is required")
    private LocalDateTime endDateTime;

    @NotNull(message = "Event status is required")
    private Event.EventStatus status;

    @NotNull(message = "Event active status is required")
    private Boolean active;
}
