package com.bookfair.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;

import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface EventStallRepository extends JpaRepository<EventStall, UUID> {

    List<EventStall> findByEventId(UUID eventId);

    List<EventStall> findByEvent(Event event);

    @EntityGraph(attributePaths = {"stall"})
    @Query("SELECT es FROM EventStall es WHERE es.event.id = :eventId")
    List<EventStall> findAllByEventIdWithStallData(@Param("eventId") UUID eventId);

    List<EventStall> findByStatus(AvailabilityStatus status);

    Optional<EventStall> findByEventIdAndStallId(
            UUID eventId,
            UUID stallId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT es FROM EventStall es WHERE es.id IN :ids")
    List<EventStall> findAllForUpdate(@Param("ids") List<UUID> ids);
}