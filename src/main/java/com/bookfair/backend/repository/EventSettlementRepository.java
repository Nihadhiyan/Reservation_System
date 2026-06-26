package com.bookfair.backend.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookfair.backend.model.EventSettlement;

@Repository
public interface EventSettlementRepository extends JpaRepository<EventSettlement, UUID> {
    Optional<EventSettlement> findByEventId(UUID eventId);
}
