package com.bookfair.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.ReservationStall;

@Repository
public interface ReservationStallRepository extends JpaRepository<ReservationStall, UUID> {

    List<ReservationStall> findByReservationId(UUID reservationId);

    boolean existsByEventStallId(UUID eventStallId);
}