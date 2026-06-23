package com.bookfair.backend.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookfair.backend.event.OrganizationDeactivatedEvent;
import com.bookfair.backend.event.user.UserDeletedEvent;
import com.bookfair.backend.event.user.UserAccountLockedEvent;
import com.bookfair.backend.event.user.UserPasswordChangedEvent;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.service.SecurityManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityEnforcementListener {

    private final UserRepository userRepository;
    private final SecurityManagementService securityManagementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSecurityEvents(Object event) {
        try {
            if (event instanceof UserDeletedEvent e) {
                securityManagementService.handleRevocation(e.userId());
            } else if (event instanceof UserPasswordChangedEvent e) {
                securityManagementService.handleRevocation(e.userId());
            } else if (event instanceof OrganizationDeactivatedEvent e) {
                userRepository.findAllByOrganizationId(e.organizationId())
                        .forEach(u -> {
                            try {
                                securityManagementService.handleRevocation(u.getId());
                            } catch (Exception ex) {
                                log.error("Failed to revoke session for user {} during organization deactivation: {}", u.getId(), ex.getMessage());
                            }
                        });
            } else if (event instanceof UserAccountLockedEvent e) {
                securityManagementService.handleRevocation(e.userId());
            }
        } catch (Exception ex) {
            log.error("Failed to handle security event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
