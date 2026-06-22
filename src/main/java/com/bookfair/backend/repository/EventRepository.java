package com.bookfair.backend.repository;

import java.time.LocalDateTime;
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

    Event findByOrganizerId(UUID orgId);

    List<Event> findByPartners_Id(UUID orgId);

    List<Event> findByStartDateTimeBeforeAndEndDateTimeAfter(
        LocalDateTime currentDate1,
        LocalDateTime currentDate2
    );

    List<Event> findByStartDateTimeBeforeAndEndDateTimeAfterAndActiveTrue(
        LocalDateTime currentDate1,
        LocalDateTime currentDate2
    );
}