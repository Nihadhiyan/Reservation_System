package com.bookfair.backend.listener;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.bookfair.backend.event.OrganizationDeactivatedEvent;
import com.bookfair.backend.event.user.UserAccountLockedEvent;
import com.bookfair.backend.event.user.UserPasswordChangedEvent;
import com.bookfair.backend.event.user.UserRegisteredEvent;
import com.bookfair.backend.event.user.PasswordResetRequestedEvent;
import com.bookfair.backend.event.user.UserEmailVerificationRequestedEvent;
import com.bookfair.backend.event.reservation.ReservationRequestReceivedEvent;
import com.bookfair.backend.event.reservation.ReservationConfirmedEvent;
import com.bookfair.backend.event.reservation.ReservationRefundPendingEvent;
import com.bookfair.backend.event.reservation.ReservationRefundedEvent;
import com.bookfair.backend.event.reservation.ReservationExpiredEvent;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.Role;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handleSecurityNotifications(Object event) {
        if (event instanceof UserAccountLockedEvent e) {
            Map<String, Object> vars = Map.of(
                "userName", e.username(),
                "supportEmail", "support@bookfair.com"
            );
            // Notify User
            notificationService.notify(e.email(), "Security Alert: Account Locked", "account_locked", vars);
            
            // Notify Admins
            List<User> admins = userRepository.findByRole(Role.SUPER_ADMIN);
            for (User admin : admins) {
                Map<String, Object> adminVars = Map.of(
                    "userName", admin.getUsername(),
                    "alertMessage", "A user account has been locked due to multiple failed login attempts.",
                    "affectedUser", e.username()
                );
                notificationService.notify(admin.getEmail(), "Admin Alert: User Account Locked", "admin_alert", adminVars);
            }
            
        } else if (event instanceof UserPasswordChangedEvent e) {
            Map<String, Object> vars = Map.of("userName", e.username());
            notificationService.notify(e.email(), "Password Changed", "password_changed", vars);
            
        } else if (event instanceof UserRegisteredEvent e) {
            Map<String, Object> vars = Map.of("userName", e.username());
            notificationService.notify(e.email(), "Welcome!", "welcome", vars);
            
        } else if (event instanceof OrganizationDeactivatedEvent e) {
            List<User> employees = userRepository.findAllByOrganizationId(e.organizationId());
            for (User employee : employees) {
                Map<String, Object> vars = Map.of(
                    "userName", employee.getUsername(),
                    "orgName", employee.getOrganization() != null ? employee.getOrganization().getName() : "Your Organization",
                    "deactivationDate", java.time.LocalDate.now().toString()
                );
                notificationService.notify(employee.getEmail(), "Organization Deactivated", "org_deactivated", vars);
            }
        } else if (event instanceof PasswordResetRequestedEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "resetLink", e.resetLink()
                );
                notificationService.notify(user.getEmail(), "Password Reset Request", "password_reset_template", vars);
            });
        } else if (event instanceof UserEmailVerificationRequestedEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "verificationLink", e.verificationLink()
                );
                notificationService.notify(user.getEmail(), "Verify Your Email", "email_verification_template", vars);
            });
        } else if (event instanceof ReservationRequestReceivedEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "eventName", e.eventName()
                );
                notificationService.notify(user.getEmail(), "Reservation Request Received", "pending", vars);
            });
        } else if (event instanceof ReservationConfirmedEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = new java.util.HashMap<>();
                vars.put("userName", user.getUsername());
                vars.put("eventName", e.eventName());
                if (e.qrCodeBase64() != null) {
                    vars.put("qrCodeBase64", e.qrCodeBase64());
                }
                notificationService.notify(user.getEmail(), "Reservation Confirmed - Your Ticket", "confirmed", vars);
            });
        } else if (event instanceof ReservationRefundPendingEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "eventName", e.eventName()
                );
                notificationService.notify(user.getEmail(), "Refund Request Received", "refund_pending", vars);
            });
        } else if (event instanceof ReservationRefundedEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "eventName", e.eventName()
                );
                notificationService.notify(user.getEmail(), "Refund Processed Successfully", "refunded", vars);
            });
        } else if (event instanceof ReservationExpiredEvent e) {
            userRepository.findById(e.userId()).ifPresent(user -> {
                Map<String, Object> vars = Map.of(
                    "userName", user.getUsername(),
                    "eventName", e.eventName()
                );
                notificationService.notify(user.getEmail(), "Reservation Expired", "expired", vars);
            });
        }
    }
}