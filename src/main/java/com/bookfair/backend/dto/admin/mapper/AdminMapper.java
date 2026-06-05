package com.bookfair.backend.dto.admin.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.admin.response.AdminDashboardResponse;
import com.bookfair.backend.dto.config.GlobalMapperConfig;

@Mapper(config = GlobalMapperConfig.class)
public interface AdminMapper {
    AdminDashboardResponse toAdminDashboardResponse(long totalUsers, long totalStalls, long activeReservations, BigDecimal totalRevenue);
}
