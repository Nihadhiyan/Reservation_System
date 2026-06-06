package com.bookfair.backend.service;

import com.bookfair.backend.dto.admin.mapper.AdminMapper;
import com.bookfair.backend.dto.admin.response.AdminDashboardResponse;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final ReservationRepository reservationRepository;
    private final AdminMapper adminMapper;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.countByActiveTrue();
        long totalStalls = stallRepository.countByActiveTrue();
        long activeReservations = reservationRepository.countByExpiresAtAfterAndStatus(LocalDateTime.now(), ReservationStatus.CONFIRMED);

        BigDecimal totalRevenue = reservationRepository.sumTotalPriceByStatus(ReservationStatus.CONFIRMED);

        return adminMapper.toAdminDashboardResponse(totalUsers, totalStalls, activeReservations, totalRevenue);
    }
    
}  