package com.bookfair.backend.service;

import com.bookfair.backend.dto.reservation.mapper.ReservationMapper;
import com.bookfair.backend.dto.reservation.request.CreateReservationRequest;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.exception.BookingExpiredException;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.exception.StallUnavailableException;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.model.ReservationStall;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Genre;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.GenreRepository;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EventStallRepository eventStallRepository;
    private final EventRepository eventRepository;
    private final QRService qrCodeService;
    private final EmailService emailService;
    private final GenreRepository genreRepository;
    private final PricingEngineService pricingEngineService;
    private final ReservationMapper reservationMapper;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest createReservationRequest) {

        User user = userRepository.findByIdAndActiveTrue(createReservationRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found", 
                    ErrorCode.USER_NOT_FOUND
                ));

        Event event = eventRepository.findByIdAndActiveTrue(createReservationRequest.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException (
                    "event not found", 
                    ErrorCode.EVENT_NOT_FOUND
                ));

        Genre genre = genreRepository.findByIdAndActiveTrue(createReservationRequest.getGenreId())
                .orElseThrow(() -> new ResourceNotFoundException (
                    "Genre not found", 
                    ErrorCode.GENRE_NOT_FOUND
                ));

        List<EventStall> stalls = eventStallRepository.findAllById(createReservationRequest.getStallIds());

        for (EventStall stall : stalls) {
            if (!stall.getStatus().name().equals("AVAILABLE")) {
                throw new StallUnavailableException(
                    "Sorry! This stall is already booked or currently in someone else's cart. Please choose another stall.", 
                    ErrorCode.STALL_UNAVAILABLE
                );
            }
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setEvent(event);
        reservation.setGenre(genre);

        reservation.setReservationStartDateTime(
                createReservationRequest.getReservationStartDateTime() != null
                        ? createReservationRequest.getReservationStartDateTime()
                        : event.getStartDateTime());

        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        reservation.setStatus(ReservationStatus.PENDING);

        List<ReservationStall> reservationStalls = stalls.stream()
                .map(s -> {

                    s.setStatus(AvailabilityStatus.BLOCKED);

                    ReservationStall rs = new ReservationStall();
                    rs.setEventStall(s);
                    rs.setReservation(reservation);
                    rs.setPriceAtBooking(pricingEngineService.calculateFinalPrice(s));

                    return rs;
                })
                .toList();

                BigDecimal totalPrice = reservationStalls.stream()
                    .map(ReservationStall::getPriceAtBooking)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        reservation.setTotalPrice(totalPrice);
        reservation.setReservedStalls(reservationStalls);

        eventStallRepository.saveAll(stalls);
        Reservation savedReservation = reservationRepository.save(reservation);

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("userName", user.getUsername());
        emailData.put("eventName", event.getName());

        emailService.sendEmail(
                user.getEmail(),
                "Reservation Request Received",
                "pending",
                emailData,
                null);

        return reservationMapper.toReservationResponse(savedReservation);

    }

    @Transactional
    public void confirmReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BookingExpiredException(
                "Your reservation timer has expired. Please start over.", 
                ErrorCode.BOOKING_EXPIRED
            );
        }

        if (!reservation.getStatus().equals(ReservationStatus.PENDING)) {
            throw new BusinessException("Can only confirm PENDING reservations.", ErrorCode.RESERVATION_FAILED);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);

        String qrPayload = "RES-" + reservation.getId();
        reservation.setQrCodePayload(qrPayload);
        String qrCodeImage = qrCodeService.generateQRCode(qrPayload);

        for (ReservationStall rs : reservation.getReservedStalls()) {
            EventStall stall = rs.getEventStall();
            stall.setStatus(AvailabilityStatus.BOOKED);
            eventStallRepository.save(stall);
        }

        reservationRepository.save(reservation);

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("userName", reservation.getUser().getUsername());
        emailData.put("eventName", reservation.getEvent().getName());

        emailService.sendEmail(
                reservation.getUser().getEmail(),
                "Reservation Confirmed - Your Ticket",
                "confirmed",
                emailData,
                qrCodeImage);
    }

    @Transactional
    public void requestCancellation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED)
                && !reservation.getStatus().equals(ReservationStatus.PENDING)) {
            throw new BusinessException("Only confirmed or pending reservations can be cancelled for a refund.", ErrorCode.REFUND_FAILED);
        }

        reservation.setStatus(ReservationStatus.REFUND_PENDING);
        reservationRepository.save(reservation);

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("userName", reservation.getUser().getUsername());
        emailData.put("eventName", reservation.getEvent().getName());

        emailService.sendEmail(
                reservation.getUser().getEmail(),
                "Refund Request Received",
                "refund_pending",
                emailData,
                null);
    }

    @Transactional
    public void approveRefund(UUID reservationId) {
        Reservation reservation = reservationRepository
                .findByIdAndStatus(reservationId, ReservationStatus.REFUND_PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getStatus().equals(ReservationStatus.REFUND_PENDING)) {
            throw new BusinessException("Reservation is not pending a refund.", ErrorCode.REFUND_FAILED);
        }

        reservation.setStatus(ReservationStatus.REFUNDED);

        for (ReservationStall rs : reservation.getReservedStalls()) {
            EventStall stall = rs.getEventStall();
            stall.setStatus(AvailabilityStatus.AVAILABLE);
            eventStallRepository.save(stall);
        }

        reservationRepository.save(reservation);

        // (Assuming we integrated Stripe here to actually return the money)

        Map<String, Object> emailData = new HashMap<>();
        emailData.put("userName", reservation.getUser().getUsername());
        emailData.put("eventName", reservation.getEvent().getName());

        emailService.sendEmail(
                reservation.getUser().getEmail(),
                "Refund Processed Successfully",
                "refunded",
                emailData,
                null);

    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(reservation -> {
                    return reservationMapper.toReservationResponse(reservation);
                }).toList();
    }

    public ReservationResponse getReservationById(UUID id) {

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        return reservationMapper.toReservationResponse(reservation);
    }

    public List<ReservationResponse> getReservationsByUser(UUID userId) {
        return reservationRepository.findByUserId(userId).stream().map(reservation -> {
            return reservationMapper.toReservationResponse(reservation);
        }).toList();
    }

}
