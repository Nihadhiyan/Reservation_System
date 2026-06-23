package com.bookfair.backend.listener;

// Spring component scanning requires the correct package structure.
// This ensures that the class is correctly picked up during application startup.
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import com.bookfair.backend.event.OrganizationCapabilityChangedEvent;
import com.bookfair.backend.event.OrganizationDeactivatedEvent;
import com.bookfair.backend.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class OrganizationEventListener {

    private final UserRepository userRepository;

    // @EventListener was removed.
    // AFTER_COMMIT is used to guarantee the organization capability updates have
    // been committed to the database before processing this event.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganizationCapabilityChanged(OrganizationCapabilityChangedEvent event) {
        userRepository.updateAllByOrganizationId(event.organizationId(), event.newCapability());
    }
}
