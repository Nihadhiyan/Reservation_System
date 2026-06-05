package com.bookfair.backend.dto.analytics.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.analytics.response.DashboardSummaryResponse;
import com.bookfair.backend.dto.analytics.response.ReservationAnalyticsResponse;
import com.bookfair.backend.dto.analytics.response.RevenueAnalyticsResponse;
import com.bookfair.backend.dto.config.GlobalMapperConfig;

@Mapper(config = GlobalMapperConfig.class)
public interface AnalyticsMapper {
    ReservationAnalyticsResponse toReservationAnalyticsResponse(ReservationAnalyticsResponse reservationAnalyticsResponse);

    RevenueAnalyticsResponse toRevenueAnalyticsResponse(RevenueAnalyticsResponse revenueAnalyticsResponse);

    DashboardSummaryResponse toDashboardSummaryResponse(DashboardSummaryResponse dashboardSummaryResponse);
}
