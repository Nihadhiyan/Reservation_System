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
import com.bookfair.backend.repository.OrganizationMemberRepository;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.model.Organization;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

        private final ReservationRepository reservationRepository;
        private final UserRepository userRepository;
        private final OrganizationMemberRepository memberRepository;
        private final OrganizationRepository organizationRepository;
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
                User user = userRepository.findByUsernameAndActiveTrue(requireNonNull(username))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                return reservationRepository.findByUserId(requireNonNull(user.getId())).stream()
                                .map(reservationMapper::toReservationResponse)
                                .toList();
        }

        @Transactional
        public ReservationResponse createReservation(CreateReservationRequest request) {
                requireNonNull(request, "request cannot be null");
                User user = userRepository.findByIdAndActiveTrue(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                Event event = eventRepository.findByIdAndActiveTrue(requireNonNull(request.getEventId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                Genre genre = genreRepository.findByIdAndActiveTrue(requireNonNull(request.getGenreId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Genre not found",
                                                ErrorCode.GENRE_NOT_FOUND));

                Organization organization = organizationRepository.findById(requireNonNull(request.getOrganizationId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                if (!memberRepository.existsByUserIdAndOrganizationId(requireNonNull(user.getId()),
                                requireNonNull(organization.getId()))) {
                        throw new BusinessException(
                                        "User must belong to the organization to make a reservation on its behalf.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                List<EventStall> stalls = eventStallRepository.findAllForUpdate(requireNonNull(request.getStallIds()));

                if (stalls.size() != request.getStallIds().size()) {
                        throw new BusinessException("One or more requested stalls could not be found.",
                                        ErrorCode.STALL_NOT_FOUND);
                }

                if (stalls.isEmpty()) {
                        throw new BusinessException("No valid stalls selected.", ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                for (EventStall stall : stalls) {
                        if (!stall.getEvent().getId().equals(event.getId())) {
                                throw new BusinessException("Stall does not belong to this event.",
                                                ErrorCode.BUSINESS_RULE_VIOLATION);
                        }

                        if (!stall.getStatus().name().equals("AVAILABLE")) {
                                throw new StallUnavailableException("Stall is already booked or blocked.",
                                                ErrorCode.STALL_UNAVAILABLE);
                        }
                }

                Instant startDateTime = request.getReservationStartDateTime() != null
                                ? request.getReservationStartDateTime()
                                : event.getStartDateTime();
                Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

                Reservation reservation = reservationMapper.toReservation(
                                user, organization, user, event, genre, startDateTime, expiresAt);

                List<ReservationStall> reservationStalls = stalls.stream()
                                .map(s -> {
                                        s.setStatus(AvailabilityStatus.BLOCKED);
                                        return reservationMapper.toReservationStall(s, reservation,
                                                        pricingEngineService.calculateFinalPrice(s));
                                })
                                .toList();

                BigDecimal totalPrice = reservationStalls.stream()
                                .map(rs -> rs.getPriceAtBooking())
                                .filter(price -> price != null)
                                .reduce(BigDecimal.ZERO, (p1, p2) -> p1.add(p2));

                reservation.setTotalPrice(totalPrice);
                reservation.setReservedStalls(reservationStalls);

                eventStallRepository.saveAll(stalls);
                Reservation savedReservation = reservationRepository.save(reservation);

                eventPublisher.publishEvent(new ReservationRequestReceivedEvent(requireNonNull(user.getId()),
                                requireNonNull(user.getUsername()),
                                requireNonNull(user.getEmail()), requireNonNull(savedReservation.getId()),
                                requireNonNull(event.getName())));

                return reservationMapper.toReservationResponse(savedReservation);
        }

        @Transactional
        public void confirmReservation(UUID reservationId) {
                Reservation reservation = reservationRepository
                                .findByIdAndStatus(requireNonNull(reservationId), ReservationStatus.PENDING)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Reservation not found",
                                                                ErrorCode.RESERVATION_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (!authorizationService.canConfirmReservation(requestingUser, reservation)) {
                        throw new ForbiddenException("You cannot manage reservations for this organization.",
                                        ErrorCode.FORBIDDEN);
                }

                if (reservation.getExpiresAt().isBefore(Instant.now())) {
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

                eventPublisher.publishEvent(new ReservationConfirmedEvent(requireNonNull(reservation.getUser().getId()),
                                requireNonNull(reservation.getUser().getUsername()),
                                requireNonNull(reservation.getUser().getEmail()),
                                requireNonNull(reservation.getId()),
                                requireNonNull(reservation.getEvent().getName()), requireNonNull(qrCodeImage)));
        }

        @Transactional
        public void requestCancellation(UUID reservationId) {
                Reservation reservation = reservationRepository.findById(requireNonNull(reservationId))
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Reservation not found",
                                                                ErrorCode.RESERVATION_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (!authorizationService.canManageReservation(requestingUser, reservation)) {
                        throw new ForbiddenException("You cannot cancel this reservation.", ErrorCode.FORBIDDEN);
                }

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                        reservation.setStatus(ReservationStatus.CANCELLED);
                        for (ReservationStall rs : reservation.getReservedStalls()) {
                                EventStall stall = rs.getEventStall();
                                stall.setStatus(AvailabilityStatus.AVAILABLE);
                                eventStallRepository.save(stall);
                        }
                        reservationRepository.save(reservation);
                        return;
                }

                if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED)) {
                        throw new BusinessException(
                                        "Only confirmed or pending reservations can be cancelled.",
                                        ErrorCode.REFUND_FAILED);
                }

                reservation.setStatus(ReservationStatus.REFUND_PENDING);
                reservationRepository.save(reservation);

                eventPublisher.publishEvent(
                                new ReservationRefundPendingEvent(requireNonNull(reservation.getUser().getId()),
                                                requireNonNull(reservation.getUser().getUsername()),
                                                requireNonNull(reservation.getUser().getEmail()),
                                                requireNonNull(reservation.getId()),
                                                requireNonNull(reservation.getEvent().getName())));
        }

        @Transactional
        public void approveRefund(UUID reservationId) {
                Reservation reservation = reservationRepository
                                .findByIdAndStatus(requireNonNull(reservationId), ReservationStatus.REFUND_PENDING)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Reservation not found",
                                                                ErrorCode.RESERVATION_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (!authorizationService.canApproveRefund(requestingUser, reservation)) {
                        throw new ForbiddenException("You cannot approve refunds for this reservation.",
                                        ErrorCode.FORBIDDEN);
                }

                reservation.setStatus(ReservationStatus.REFUNDED);

                for (ReservationStall rs : reservation.getReservedStalls()) {
                        EventStall stall = rs.getEventStall();
                        stall.setStatus(AvailabilityStatus.AVAILABLE);
                        eventStallRepository.save(stall);
                }

                reservationRepository.save(reservation);

                eventPublisher.publishEvent(
                                new ReservationRefundedEvent(requireNonNull(reservation.getUser().getId()),
                                                requireNonNull(reservation.getUser().getUsername()),
                                                requireNonNull(reservation.getUser().getEmail()),
                                                requireNonNull(reservation.getId()),
                                                requireNonNull(reservation.getEvent().getName())));
        }

        @Transactional(readOnly = true)
        public Page<ReservationResponse> getAllReservations(Pageable pageable) {
                return reservationRepository.findAll(requireNonNull(pageable))
                                .map(reservationMapper::toReservationResponse);
        }

        @Transactional(readOnly = true)
        public ReservationResponse getReservationById(UUID id) {
                Reservation reservation = reservationRepository.findById(requireNonNull(id))
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Reservation not found",
                                                                ErrorCode.RESERVATION_NOT_FOUND));

                checkReadAccess(reservation);

                return reservationMapper.toReservationResponse(reservation);
        }

        @Transactional(readOnly = true)
        public ReservationDetailResponse getReservationDetails(UUID id) {
                Reservation reservation = reservationRepository.findById(requireNonNull(id))
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Reservation not found",
                                                                ErrorCode.RESERVATION_NOT_FOUND));

                checkReadAccess(reservation);

                return reservationMapper.toReservationDetailResponse(reservation);
        }

        private void checkReadAccess(Reservation reservation) {
                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (!authorizationService.canViewReservation(requestingUser, reservation)) {
                        throw new ForbiddenException("You do not have permission to view this reservation.",
                                        ErrorCode.FORBIDDEN);
                }
        }

        private UUID getCurrentUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
                        return userId;
                }

                if (authentication != null && authentication.getPrincipal() instanceof String userIdString) {
                        return UUID.fromString(userIdString);
                }

                throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
        }
}
