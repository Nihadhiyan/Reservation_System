package com.bookfair.backend.service;

import com.bookfair.backend.dto.response.AdminDashBoardResponse;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final ReservationRepository reservationRepository;

    public AdminService(UserRepository userRepository, StallRepository stallRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.stallRepository = stallRepository;
        this.reservationRepository = reservationRepository;
    }

    public AdminDashBoardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalStalls = stallRepository.count();
        long activeReservations = reservationRepository.findByStatus(ReservationStatus.CONFIRMED).stream().filter(reservation -> {
            return reservation.getDate().isAfter(java.time.LocalDate.now());
        }).count();
        
        AdminDashBoardResponse adminResponse = new AdminDashBoardResponse();

        adminResponse.setTotalUsers(totalUsers);
        adminResponse.setTotalStalls(totalStalls);
        adminResponse.setActiveReservations(activeReservations);

        return adminResponse;
    }

    public String promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);

        return ("User " + user.getUsername() + " promoted to admin");
    }
    
}  