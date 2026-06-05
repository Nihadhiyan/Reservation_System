package com.bookfair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    
    List<Reservation> findByUserId(UUID userId);
        
    List<Reservation> findByDate(LocalDate date);
    
    List<Reservation> findByUserIdAndStatus(UUID userId, Reservation.ReservationStatus status);

    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    
    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    Optional<Reservation> findByIdAndStatus(UUID reservationId, Reservation.ReservationStatus status);

    List<Reservation> findByBookFairId(UUID bookFairId);

    List<Reservation> findByExpiresAtBeforeAndStatus(LocalDateTime expiresAt, Reservation.ReservationStatus status);

}
