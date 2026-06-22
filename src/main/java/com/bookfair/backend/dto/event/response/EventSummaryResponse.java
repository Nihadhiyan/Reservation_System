package com.bookfair.backend.dto.event.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryResponse {
    private UUID id;
    private String name;
    private String eventType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Boolean active;













}
