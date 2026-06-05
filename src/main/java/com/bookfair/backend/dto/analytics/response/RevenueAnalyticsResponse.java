package com.bookfair.backend.dto.analytics.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsResponse {

    private BigDecimal totalRevenue;

    private Long completedPayments;

    private Long failedPayments;

    private Long refundedPayments;

    private String currency;

}
