package com.bookfair.backend.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookfair.backend.event.user.UserUpdatedEvent;
import com.bookfair.backend.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserCacheListener {
    private final CustomUserDetailsService userDetailsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        userDetailsService.evictUserDetails(event.userId(), event.username());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        userDetailsService.evictUserDetails(event.userId(), event.username());
    }
}
