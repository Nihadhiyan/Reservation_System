package com.bookfair.backend.dto.analytics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private ReservationAnalyticsResponse reservationAnalytics;

    private RevenueAnalyticsResponse revenueAnalytics;
}
