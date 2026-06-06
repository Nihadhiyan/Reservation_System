package com.bookfair.backend.service;

import com.bookfair.backend.dto.admin.mapper.AdminMapper;
import com.bookfair.backend.dto.admin.response.AdminDashboardResponse;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final ReservationRepository reservationRepository;
    private final AdminMapper adminMapper;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.findAllByActiveTrue().size();
        long totalStalls = stallRepository.findAllByActiveTrue().size();
        long activeReservations = reservationRepository.findByExpiresAtBeforeAndStatus(LocalDateTime.now(), ReservationStatus.CONFIRMED).size();
        List<Reservation> reservations = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for(Reservation reservation : reservations) {
            totalRevenue = totalRevenue.add(reservation.getTotalPrice());
        }

        return adminMapper.toAdminDashboardResponse(totalUsers, totalStalls, activeReservations, totalRevenue);
    }
    
}  