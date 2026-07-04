package com.bookfair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Reservation.ReservationStatus;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.repository.query.Param;

import com.bookfair.backend.model.User;

import java.math.BigDecimal;
import java.time.Instant;
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

    List<Reservation> findByEventId(UUID eventId);

    @Query("SELECT DISTINCT r FROM Reservation r JOIN r.reservedStalls rs WHERE rs.eventStall.id = :eventStallId AND r.status IN :statuses")
    List<Reservation> findByEventStallIdAndStatusIn(@Param("eventStallId") UUID eventStallId, @Param("statuses") List<ReservationStatus> statuses);

    List<Reservation> findByExpiresAtBeforeAndStatus(Instant expiresAt, Reservation.ReservationStatus status);

    long countByExpiresAtAfterAndStatus(Instant date, ReservationStatus status);

    @Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r WHERE r.status = :status")
    BigDecimal sumTotalPriceByStatus(@Param("status") ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT r FROM Reservation r WHERE r.id = :id AND r.status = :status")
    Optional<Reservation> findByIdAndStatusForUpdate(@Param("id") UUID id, @Param("status") ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);
}
