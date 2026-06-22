package com.bookfair.backend.dto.reservation.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class ReservationResponse {
    private UUID id;
    private SimpleUserDto user;
    private SimpleEventDto event;
    private LocalDate date;
    private LocalDateTime reservationStartDateTime;
    private LocalDateTime expiresAt;
    private LocalTime time;
    private String status;
    private UUID genreId;
    private String qrCodePayload;
}
