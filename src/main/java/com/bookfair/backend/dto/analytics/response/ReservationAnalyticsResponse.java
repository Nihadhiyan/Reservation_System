package com.bookfair.backend.dto.analytics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationAnalyticsResponse {

    private Long totalReservations;

    private Long confirmedReservations;

    private Long cancelledReservations;
    
    private Long pendingReservations;

}
