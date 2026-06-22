package com.bookfair.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.ReservationStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {
    private final ReservationRepository reservationRepository;
    private final EventStallRepository eventStallRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository
            .findByExpiresAtBeforeAndStatus(LocalDateTime.now(), ReservationStatus.PENDING);

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations. Releasing stalls back to the public...", expiredReservations.size());

        List<EventStall> stallsToRelease = new ArrayList<>();

        for (Reservation reservation : expiredReservations) {

            reservation.setStatus(ReservationStatus.CANCELLED);

            for (ReservationStall rs : reservation.getReservedStalls()) {
                EventStall eventStall = rs.getEventStall();
                eventStall.setStatus(AvailabilityStatus.AVAILABLE);
                stallsToRelease.add(eventStall);
            }
        }

        eventStallRepository.saveAll(stallsToRelease);
        reservationRepository.saveAll(expiredReservations);

        log.info("Successfully released {} stalls from {} expired reservations.", stallsToRelease.size(), expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("userName", reservation.getUser().getUsername());
            emailData.put("eventName", reservation.getEvent().getName());

            emailService.sendEmail(
                reservation.getUser().getEmail(), 
                "Reservation Expired", 
                "expired", 
                emailData, 
                null
            );
        }

    }
}
