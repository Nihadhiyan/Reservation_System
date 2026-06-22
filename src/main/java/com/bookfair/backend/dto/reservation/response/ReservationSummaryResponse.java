package com.bookfair.backend.dto.reservation.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bookfair.backend.dto.common.SimpleEventDto;
import com.bookfair.backend.dto.common.SimpleUserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryResponse {
    private UUID id;
    private SimpleUserDto user;
    private SimpleEventDto bookFair;
    private LocalDate date;
    private String status;
    private Integer totalStalls;
    private BigDecimal totalAmount;

}
