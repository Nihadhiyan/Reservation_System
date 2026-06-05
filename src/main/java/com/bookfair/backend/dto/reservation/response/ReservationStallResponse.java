package com.bookfair.backend.dto.reservation.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.bookfair.backend.dto.common.SimpleStallDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStallResponse {
    private UUID id;
    private SimpleStallDto stall;
    private BigDecimal priceAtBooking;
}
