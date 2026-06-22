package com.bookfair.backend.dto.reservation.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
public class ReservationDetailResponse {
    private UUID id;
    private SimpleUserDto user;
    private SimpleEventDto event;
    private LocalDate date;
    private LocalDateTime reservationStartDateTime;
    private LocalDateTime expiresAt;
    private LocalTime time;
    private String status;
    private BigDecimal totalAmount;
    private List<ReservationStallResponse> stalls;
    private UUID organizationId;
    private String organizationName;
    private UUID reservationCreatedByUserId;
    private String reservationCreatedByUsername;
}
