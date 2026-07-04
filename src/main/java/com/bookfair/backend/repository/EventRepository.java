package com.bookfair.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.Event.EventStatus;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatusAndActiveTrue(EventStatus status);

    Optional<Event> findByIdAndActiveTrue(UUID id);

    List<Event> findByOrganizerId(UUID orgId);

    List<Event> findByVenueIdAndActiveTrue(UUID venueId);

    List<Event> findByPartners_Id(UUID orgId);

    List<Event> findByStartDateTimeBeforeAndEndDateTimeAfter(
        Instant currentDate1,
        Instant currentDate2
    );

    List<Event> findByStartDateTimeBeforeAndEndDateTimeAfterAndActiveTrue(
        Instant currentDate1,
        Instant currentDate2
    );
}