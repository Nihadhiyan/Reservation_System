package com.bookfair.backend.dto.admin.response;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Implements Serializable to prevent Redis/Jackson caching crashes
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse implements Serializable {

    private long totalUsers;

    private long totalStalls;

    private long activeReservations;

    private BigDecimal totalRevenue;
}
