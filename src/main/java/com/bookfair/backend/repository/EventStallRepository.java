package com.bookfair.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;

@Repository
public interface EventStallRepository extends JpaRepository<EventStall, UUID> {

    List<EventStall> findByEventId(UUID eventId);

    List<EventStall> findByEvent(Event event);

    @Query("SELECT es FROM EventStall es JOIN FETCH es.stall WHERE es.event.id = :eventId")
    List<EventStall> findAllByEventIdWithStallData(@Param("eventId") UUID eventId);

    List<EventStall> findByStatus(AvailabilityStatus status);

    Optional<EventStall> findByEventIdAndStallId(
        UUID eventId,
        UUID stallId
    );
}