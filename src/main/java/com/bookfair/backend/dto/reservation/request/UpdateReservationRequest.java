package com.bookfair.backend.dto.reservation.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationRequest {
    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Book fair id is required")
    private UUID bookFairId;

    @NotEmpty(message = "At least one stall id is required")
    private List<UUID> stallIds;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Reservation start time is required")
    private LocalDateTime reservationStartTime;

    @NotNull(message = "Expiration time is required")
    private LocalDateTime expiresAt;

    @NotNull(message = "Time is required")
    private LocalTime time;

    @NotBlank(message = "Status is required")
    private String status;

    @NotNull(message = "Genre id is required")
    private UUID genreId;
    
    @NotBlank(message = "Qr code payload is required")
    private String qrCodePayload;





















}
