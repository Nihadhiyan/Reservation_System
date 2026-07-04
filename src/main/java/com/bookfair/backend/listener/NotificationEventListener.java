package com.bookfair.backend.listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.bookfair.backend.event.organization.OrganizationDeactivatedEvent;
import com.bookfair.backend.event.user.UserAccountLockedEvent;
import com.bookfair.backend.event.user.UserPasswordChangedEvent;
import com.bookfair.backend.event.user.UserRegisteredEvent;
import com.bookfair.backend.event.user.PasswordResetRequestedEvent;
import com.bookfair.backend.event.user.UserEmailVerificationRequestedEvent;
import com.bookfair.backend.event.user.UserRoleUpdatedEvent;
import com.bookfair.backend.event.user.UserEmailVerifiedEvent;
import com.bookfair.backend.event.reservation.ReservationRequestReceivedEvent;
import com.bookfair.backend.event.reservation.ReservationConfirmedEvent;
import com.bookfair.backend.event.reservation.ReservationRefundPendingEvent;
import com.bookfair.backend.event.reservation.ReservationRefundedEvent;
import com.bookfair.backend.event.reservation.ReservationExpiredEvent;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.SystemRole;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.OrganizationMember.OrganizationRole;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.repository.OrganizationMemberRepository;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.VenueRepository;
import com.bookfair.backend.event.user.UserUpdatedEvent;
import com.bookfair.backend.event.cache.OrganizationUpdatedEvent;
import com.bookfair.backend.event.cache.VenueUpdatedEvent;
import com.bookfair.backend.event.hierarchy.VenueDeactivatedEvent;
import com.bookfair.backend.event.hierarchy.EventDeactivatedEvent;
import com.bookfair.backend.event.reservation.ReservationCancelledByAdminEvent;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.service.NotificationService;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handleSecurityNotifications(Object event) {
        requireNonNull(event, "Event cannot be null");
        switch (event) {
            case UserAccountLockedEvent e -> {
                Map<String, Object> vars = Map.of(
                        "userName", e.username(),
                        "supportEmail", "[EMAIL_ADDRESS]");
                // Notify User
                notificationService.notify(e.email(), "Security Alert: Account Locked", "account_locked", vars);

                // Notify Admins
                List<User> admins = userRepository.findBySystemRole(SystemRole.SUPER_ADMIN);
                for (User admin : admins) {
                    Map<String, Object> adminVars = Map.of(
                            "userName", admin.getUsername(),
                            "alertMessage", "A user account has been locked due to multiple failed login attempts.",
                            "affectedUser", e.username());
                    notificationService.notify(admin.getEmail(), "Admin Alert: User Account Locked", "admin_alert",
                            adminVars);
                }
            }
            case UserPasswordChangedEvent e -> {
                Map<String, Object> vars = Map.of("userName", e.username());
                notificationService.notify(e.email(), "Password Changed", "password_changed", vars);
            }
            case UserRegisteredEvent e -> {
                Map<String, Object> vars = Map.of("userName", e.username());
                notificationService.notify(e.email(), "Welcome!", "welcome", vars);
            }
            case OrganizationDeactivatedEvent e -> {
                List<OrganizationMember> members = memberRepository.findByOrganizationId(e.organizationId());
                for (OrganizationMember member : members) {
                    User employee = member.getUser();
                    Map<String, Object> vars = Map.of(
                            "userName", employee.getUsername(),
                            "orgName",
                            member.getOrganization() != null ? member.getOrganization().getName()
                                    : "Your Organization",
                            "deactivationDate", java.time.LocalDate.now().toString());
                    notificationService.notify(employee.getEmail(), "Organization Deactivated", "org_deactivated",
                            vars);
                }
            }
            case PasswordResetRequestedEvent e -> {
                userRepository.findById(Objects.requireNonNull(e.userId())).ifPresent(user -> {
                    Map<String, Object> vars = Map.of(
                            "userName", user.getUsername(),
                            "resetLink", e.resetLink());
                    notificationService.notify(user.getEmail(), "Password Reset Request", "password_reset_template",
                            vars);
                });
            }
            case UserEmailVerificationRequestedEvent e -> {
                userRepository.findById(Objects.requireNonNull(e.userId())).ifPresent(user -> {
                    Map<String, Object> vars = Map.of(
                            "userName", user.getUsername(),
                            "verificationLink", e.verificationLink());
                    notificationService.notify(user.getEmail(), "Verify Your Email", "email_verification_template",
                            vars);
                });
            }
            case ReservationRequestReceivedEvent e ->
                notificationService.notify(e.email(), "Reservation Request Received", "pending",
                        Map.of("userName", e.username(), "eventName", e.eventName()));

            case ReservationConfirmedEvent e -> {
                Map<String, Object> vars = new java.util.HashMap<>();
                vars.put("userName", e.username());
                vars.put("eventName", e.eventName());
                if (e.qrCodeBase64() != null) {
                    vars.put("qrCodeBase64", e.qrCodeBase64());
                }
                notificationService.notify(e.email(), "Reservation Confirmed - Your Ticket", "confirmed", vars);
            }

            case ReservationRefundPendingEvent e ->
                notificationService.notify(e.email(), "Refund Request Received", "refund_pending",
                        Map.of("userName", e.username(), "eventName", e.eventName()));

            case ReservationRefundedEvent e ->
                notificationService.notify(e.email(), "Refund Processed Successfully", "refunded",
                        Map.of("userName", e.username(), "eventName", e.eventName()));

            case ReservationExpiredEvent e ->
                notificationService.notify(e.email(), "Reservation Expired", "expired",
                        Map.of("userName", e.username(), "eventName", e.eventName()));

            case UserRoleUpdatedEvent e ->
                notificationService.notify(e.email(), "Role Updated", "role_updated",
                        Map.of("userName", e.username(), "oldRole", e.oldRole(), "newRole", e.newRole()));

            case UserEmailVerifiedEvent e -> {
                Map<String, Object> vars = Map.of(
                        "userName", e.username());
                notificationService.notify(e.email(), "Email Verified", "email_verified", vars);
            }
            case UserUpdatedEvent e -> {
                Map<String, Object> vars = Map.of(
                        "userName", e.username(),
                        "entityType", "User",
                        "alertMessage", "Your user profile details have been updated.");
                notificationService.notify(e.email(), "Profile Details Updated", "update_alert", vars);
            }
            case OrganizationUpdatedEvent e -> {
                organizationRepository.findById(requireNonNull(e.organizationId(), "Organization ID cannot be null"))
                        .ifPresent(org -> {
                            List<OrganizationMember> members = memberRepository
                                    .findByOrganizationId(requireNonNull(org.getId()));
                            for (OrganizationMember member : members) {
                                if (member.getRole() == OrganizationRole.ORG_ADMIN && member.getUser() != null) {
                                    User admin = member.getUser();
                                    Map<String, Object> vars = Map.of(
                                            "userName", admin.getUsername(),
                                            "entityType", "Organization",
                                            "alertMessage",
                                            "Your organization profile or capabilities have been updated: "
                                                    + org.getName());
                                    notificationService.notify(admin.getEmail(), "Organization Profile Updated",
                                            "update_alert",
                                            vars);
                                }
                            }
                        });
            }
            case VenueUpdatedEvent e -> {
                venueRepository.findById(requireNonNull(e.venueId(), "Venue ID cannot be null")).ifPresent(venue -> {
                    Organization owner = venue.getOwner();
                    if (owner != null) {
                        List<OrganizationMember> members = memberRepository.findByOrganizationId(owner.getId());
                        for (OrganizationMember member : members) {
                            if (member.getRole() == OrganizationRole.ORG_ADMIN && member.getUser() != null) {
                                User admin = member.getUser();
                                Map<String, Object> vars = Map.of(
                                        "userName", admin.getUsername(),
                                        "entityType", "Venue",
                                        "alertMessage", "Your venue details have been updated: " + venue.getName());
                                notificationService.notify(admin.getEmail(), "Venue Details Updated", "update_alert",
                                        vars);
                            }
                        }
                    }
                });
            }
            case VenueDeactivatedEvent e -> {
                venueRepository.findById(requireNonNull(e.venueId(), "Venue ID cannot be null")).ifPresent(venue -> {
                    Organization owner = venue.getOwner();
                    if (owner != null) {
                        List<OrganizationMember> members = memberRepository.findByOrganizationId(owner.getId());
                        for (OrganizationMember member : members) {
                            if (member.getRole() == OrganizationRole.ORG_ADMIN && member.getUser() != null) {
                                User admin = member.getUser();
                                Map<String, Object> vars = Map.of(
                                        "userName", admin.getUsername(),
                                        "entityType", "Venue",
                                        "alertMessage", "Your venue has been deactivated: " + venue.getName());
                                notificationService.notify(admin.getEmail(), "Venue Deactivated Notice", "venue_deactivated", vars);
                            }
                        }
                    }
                });
            }
            case EventDeactivatedEvent e -> {
                eventRepository.findById(requireNonNull(e.eventId(), "Event ID cannot be null")).ifPresent(ev -> {
                    Organization organizer = ev.getOrganizer();
                    if (organizer != null) {
                        List<OrganizationMember> members = memberRepository.findByOrganizationId(organizer.getId());
                        for (OrganizationMember member : members) {
                            if (member.getRole() == OrganizationRole.ORG_ADMIN && member.getUser() != null) {
                                User admin = member.getUser();
                                Map<String, Object> vars = Map.of(
                                        "userName", admin.getUsername(),
                                        "eventName", ev.getName(),
                                        "alertMessage", "Your event '" + ev.getName() + "' has been deactivated due to administrative action or venue closure.");
                                notificationService.notify(admin.getEmail(), "Event Deactivated Notice", "event_deactivated", vars);
                            }
                        }
                    }
                });
            }
            case ReservationCancelledByAdminEvent e -> {
                reservationRepository.findById(requireNonNull(e.reservationId(), "Reservation ID cannot be null")).ifPresent(res -> {
                    User user = res.getUser();
                    if (user != null) {
                        Map<String, Object> vars = Map.of(
                                "userName", user.getUsername(),
                                "eventName", res.getEvent().getName(),
                                "reservationId", res.getId().toString(),
                                "reason", e.reason() != null ? e.reason() : "Administrative closure",
                                "refundMessage", "Please note that if you made a payment for this reservation, a full refund is currently being processed to your original payment method.");
                        notificationService.notify(user.getEmail(), "Reservation Cancellation Notice - Refund Initiated", "reservation_cancelled_admin", vars);
                    }
                });
            }
            default -> {
                log.debug("Ignored event type in notification listener: {}", event.getClass().getSimpleName());
            }
        }
    }
}