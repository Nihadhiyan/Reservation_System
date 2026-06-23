package com.bookfair.backend.service;

import com.bookfair.backend.dto.reservation.mapper.ReservationMapper;
import com.bookfair.backend.dto.reservation.request.CreateReservationRequest;
import com.bookfair.backend.dto.reservation.response.ReservationDetailResponse;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.exception.BookingExpiredException;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ForbiddenException;
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
import com.bookfair.backend.event.reservation.ReservationConfirmedEvent;
import com.bookfair.backend.event.reservation.ReservationRefundPendingEvent;
import com.bookfair.backend.event.reservation.ReservationRefundedEvent;
import com.bookfair.backend.event.reservation.ReservationRequestReceivedEvent;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.GenreRepository;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GenreRepository genreRepository;
    private final PricingEngineService pricingEngineService;
    private final ReservationMapper reservationMapper;
    private final ReservationAuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String username) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        return reservationRepository.findByUserId(user.getId()).stream()
                .map(reservationMapper::toReservationResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        User user = userRepository.findByIdAndActiveTrue(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        Event event = eventRepository.findByIdAndActiveTrue(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        Genre genre = genreRepository.findByIdAndActiveTrue(request.getGenreId())
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found", ErrorCode.GENRE_NOT_FOUND));

        if (user.getOrganization() == null) {
            throw new BusinessException("User must belong to an organization to make a reservation.",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        List<EventStall> stalls = eventStallRepository.findAllForUpdate(request.getStallIds());

        if (stalls.size() != request.getStallIds().size()) {
            throw new BusinessException("One or more requested stalls could not be found.", ErrorCode.STALL_NOT_FOUND);
        }

        if (stalls.isEmpty()) {
            throw new BusinessException("No valid stalls selected.", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        for (EventStall stall : stalls) {
            if (!stall.getEvent().getId().equals(event.getId())) {
                throw new BusinessException("Stall does not belong to this event.", ErrorCode.BUSINESS_RULE_VIOLATION);
            }

            if (!stall.getStatus().name().equals("AVAILABLE")) {
                throw new StallUnavailableException("Stall is already booked or blocked.", ErrorCode.STALL_UNAVAILABLE);
            }
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setOrganization(user.getOrganization());
        reservation.setReservationCreatedBy(user);
        reservation.setEvent(event);
        reservation.setGenre(genre);

        reservation.setReservationStartDateTime(
                request.getReservationStartDateTime() != null
                        ? request.getReservationStartDateTime()
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

        eventPublisher.publishEvent(new ReservationRequestReceivedEvent(user.getId(), event.getName()));

        return reservationMapper.toReservationResponse(savedReservation);
    }

    @Transactional
    public void confirmReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.PENDING)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!authorizationService.canConfirmReservation(requestingUser, reservation)) {
            throw new ForbiddenException("You cannot manage reservations for this organization.", ErrorCode.FORBIDDEN);
        }

        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BookingExpiredException("Your reservation timer has expired. Please start over.",
                    ErrorCode.BOOKING_EXPIRED);
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

        eventPublisher.publishEvent(new ReservationConfirmedEvent(reservation.getUser().getId(), reservation.getEvent().getName(), qrCodeImage));
    }

    @Transactional
    public void requestCancellation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!authorizationService.canManageReservation(requestingUser, reservation)) {
            throw new ForbiddenException("You cannot cancel this reservation.", ErrorCode.FORBIDDEN);
        }

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED)
                && !reservation.getStatus().equals(ReservationStatus.PENDING)) {
            throw new BusinessException("Only confirmed or pending reservations can be cancelled for a refund.",
                    ErrorCode.REFUND_FAILED);
        }

        reservation.setStatus(ReservationStatus.REFUND_PENDING);
        reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationRefundPendingEvent(reservation.getUser().getId(), reservation.getEvent().getName()));
    }

    @Transactional
    public void approveRefund(UUID reservationId) {
        Reservation reservation = reservationRepository
                .findByIdAndStatus(reservationId, ReservationStatus.REFUND_PENDING)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!authorizationService.canApproveRefund(requestingUser, reservation)) {
            throw new ForbiddenException("You cannot approve refunds for this reservation.", ErrorCode.FORBIDDEN);
        }

        reservation.setStatus(ReservationStatus.REFUNDED);

        for (ReservationStall rs : reservation.getReservedStalls()) {
            EventStall stall = rs.getEventStall();
            stall.setStatus(AvailabilityStatus.AVAILABLE);
            eventStallRepository.save(stall);
        }

        reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationRefundedEvent(reservation.getUser().getId(), reservation.getEvent().getName()));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable)
                .map(reservationMapper::toReservationResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        checkReadAccess(reservation);

        return reservationMapper.toReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetails(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Reservation not found", ErrorCode.RESERVATION_NOT_FOUND));

        checkReadAccess(reservation);

        return reservationMapper.toReservationDetailResponse(reservation);
    }

    private void checkReadAccess(Reservation reservation) {
        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!authorizationService.canViewReservation(requestingUser, reservation)) {
            throw new ForbiddenException("You do not have permission to view this reservation.", ErrorCode.FORBIDDEN);
        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }
}
