package com.bookfair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Reservation.ReservationStatus;

import io.lettuce.core.dynamic.annotation.Param;

import com.bookfair.backend.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    
    List<Reservation> findByUserId(UUID userId);
    
    List<Reservation> findByUserIdAndStatus(UUID userId, Reservation.ReservationStatus status);

    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    
    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    Optional<Reservation> findByIdAndStatus(UUID reservationId, Reservation.ReservationStatus status);

    Optional<Reservation> findById(UUID id);

    List<Reservation> findByBookFairId(UUID bookFairId);

    List<Reservation> findByExpiresAtBeforeAndStatus(LocalDateTime expiresAt, Reservation.ReservationStatus status);

    long countByExpiresAtAfterAndStatus(LocalDateTime date, ReservationStatus status);

    @Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r WHERE r.status = :status")
    BigDecimal sumTotalPriceByStatus(@Param("status") ReservationStatus status);

}
