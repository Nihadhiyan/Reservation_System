package com.bookfair.backend.dto.common;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleEventDto {
    private UUID id;

    private String name;

    private LocalDate startDate;
    
    private LocalDate endDate;
}
