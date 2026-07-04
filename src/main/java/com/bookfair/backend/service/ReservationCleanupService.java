package com.bookfair.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.ReservationStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.event.cache.EventStallUpdatedEvent;
import com.bookfair.backend.event.reservation.ReservationExpiredEvent;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.ReservationRepository;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {
    private final ReservationRepository reservationRepository;
    private final EventStallRepository eventStallRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository
                .findByExpiresAtBeforeAndStatus(Instant.now(), ReservationStatus.PENDING);

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations. Releasing stalls back to the public...", expiredReservations.size());

        List<EventStall> stallsToRelease = new ArrayList<>();
        List<Reservation> confirmedExpired = new ArrayList<>();
        Set<UUID> affectedEventIds = new HashSet<>();

        for (Reservation res : expiredReservations) {
            java.util.Optional<Reservation> lockedOpt = reservationRepository.findByIdAndStatusForUpdate(res.getId(), ReservationStatus.PENDING);
            if (lockedOpt.isEmpty()) {
                continue;
            }
            Reservation reservation = lockedOpt.get();
            reservation.setStatus(ReservationStatus.EXPIRED);
            confirmedExpired.add(reservation);

            for (ReservationStall rs : reservation.getReservedStalls()) {
                EventStall eventStall = rs.getEventStall();
                eventStall.setStatus(AvailabilityStatus.AVAILABLE);
                stallsToRelease.add(eventStall);
                affectedEventIds.add(eventStall.getEvent().getId());
            }
        }

        if (confirmedExpired.isEmpty()) {
            return;
        }

        eventStallRepository.saveAll(stallsToRelease);
        reservationRepository.saveAll(confirmedExpired);

        log.info("Successfully released {} stalls from {} expired reservations.", stallsToRelease.size(),
                confirmedExpired.size());

        for (Reservation reservation : confirmedExpired) {

            eventPublisher.publishEvent(new ReservationExpiredEvent(reservation.getUser().getId(), reservation.getUser().getUsername(), reservation.getUser().getEmail(), reservation.getId(), reservation.getEvent().getName()));
        }

        for (UUID eventId : affectedEventIds) {
            eventPublisher.publishEvent(new EventStallUpdatedEvent(eventId));
        }

    }
}
