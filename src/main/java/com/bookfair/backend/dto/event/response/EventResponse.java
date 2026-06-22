package com.bookfair.backend.dto.event.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.bookfair.backend.dto.common.SimpleOrganizationDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String name;
    private String eventType;
    private SimpleOrganizationDto organizer;
    private UUID venueId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
    private Boolean active;
}
