package com.bookfair.backend.service;

import com.bookfair.backend.dto.admin.mapper.AdminMapper;
import com.bookfair.backend.dto.admin.response.AdminDashboardResponse;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final ReservationRepository reservationRepository;
    private final AdminMapper adminMapper;

    // Upgraded to AtomicBoolean for thread safety without needing a database table
    // yet
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.countByActiveTrue();
        long totalStalls = stallRepository.countByActiveTrue();
        long activeReservations = reservationRepository
                .countByExpiresAtAfterAndStatus(LocalDateTime.now(), ReservationStatus.CONFIRMED);

        // Null protection for JPQL SUM() aggregation
        BigDecimal totalRevenue = Optional.ofNullable(
                reservationRepository.sumTotalPriceByStatus(ReservationStatus.CONFIRMED)).orElse(BigDecimal.ZERO);

        return adminMapper.toAdminDashboardResponse(
                totalUsers, totalStalls, activeReservations, totalRevenue);
    }

    // Service-level security enforcement
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void toggleMaintenanceMode() {
        // Thread-safe state toggle
        boolean currentMode = this.maintenanceMode.get();
        this.maintenanceMode.set(!currentMode);

        log.info("System maintenance mode toggled to: {}", this.maintenanceMode.get());
    }

    public boolean isMaintenanceMode() {
        return this.maintenanceMode.get();
    }
}