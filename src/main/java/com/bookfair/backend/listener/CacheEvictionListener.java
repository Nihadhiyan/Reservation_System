package com.bookfair.backend.listener;

import static java.util.Objects.*;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookfair.backend.event.cache.GenreUpdatedEvent;
import com.bookfair.backend.event.cache.HallUpdatedEvent;
import com.bookfair.backend.event.cache.PricingRuleUpdatedEvent;
import com.bookfair.backend.event.cache.VenueUpdatedEvent;
import com.bookfair.backend.event.cache.OrganizationUpdatedEvent;
import com.bookfair.backend.event.cache.EventUpdatedEvent;
import com.bookfair.backend.event.user.UserUpdatedEvent;
import com.bookfair.backend.event.cache.EventStallUpdatedEvent;
import com.bookfair.backend.event.cache.LayoutUpdatedEvent;
import com.bookfair.backend.event.hierarchy.VenueDeactivatedEvent;
import com.bookfair.backend.event.stall.StallCreatedEvent;
import com.bookfair.backend.event.stall.StallStatusChangedEvent;

import org.springframework.lang.NonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheEvictionListener {

    private final CacheManager cacheManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPricingRuleUpdated(PricingRuleUpdatedEvent event) {
        log.info("Evicting pricing rule cache after commit for rule ID: {}", event.ruleId());
        evictCache("pricingRules");
        evictCache("activeRules");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVenueUpdated(VenueUpdatedEvent event) {
        log.info("Evicting venue cache after commit for venue ID: {}", event.venueId());
        evictCache("venues");
        evictCache("venueMap");
        evictCacheEntry("venue", requireNonNull(event.venueId(), "Venue ID cannot be null"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHallUpdated(HallUpdatedEvent event) {
        log.info("Evicting hall cache after commit for hall ID: {}", event.hallId());
        evictCache("halls");
        evictCache("hallLayout");
        evictCacheEntry("hall", requireNonNull(event.hallId(), "Hall ID cannot be null"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGenreUpdated(GenreUpdatedEvent event) {
        log.info("Evicting genre cache after commit for genre ID: {}", event.genreId());
        evictCache("genres");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVenueDeactivated(VenueDeactivatedEvent event) {
        log.info("Evicting venue cache after deactivation commit for venue ID: {}", event.venueId());
        evictCache("venues");
        evictCache("venueMap");
        evictCacheEntry("venue", requireNonNull(event.venueId(), "Venue ID cannot be null"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganizationUpdated(OrganizationUpdatedEvent event) {
        log.info("Evicting organizations, events, and userProfiles cache after commit for organization ID: {}",
                event.organizationId());
        evictCache("organizations");
        evictCache("events");
        evictCache("userProfiles");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventUpdated(EventUpdatedEvent event) {
        log.info("Evicting events and eventStalls cache after commit for event ID: {}", event.eventId());
        evictCache("events");
        evictCache("eventStalls");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserUpdated(UserUpdatedEvent event) {
        log.info("Evicting userProfiles cache after commit for user ID: {} / username: {}", event.userId(),
                event.username());
        evictCache("userProfiles");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventStallUpdated(EventStallUpdatedEvent event) {
        log.info("Evicting eventStalls cache after commit for event ID: {}", event.eventId());
        evictCache("eventStalls");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLayoutUpdated(LayoutUpdatedEvent event) {
        log.info("Evicting hallLayout cache after commit for hall ID: {}", event.hallId());
        evictCache("hallLayout");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStallCreated(StallCreatedEvent event) {
        log.info("Evicting hallLayout cache after commit for created stall ID: {}", event.stallId());
        evictCache("hallLayout");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStallStatusChanged(StallStatusChangedEvent event) {
        log.info("Evicting hallLayout cache after commit for status change on stall ID: {}", event.stallId());
        evictCache("hallLayout");
    }

    private void evictCache(@NonNull String cacheName) {
        if (cacheManager != null && cacheName != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        }
    }

    private void evictCacheEntry(@NonNull String cacheName, @NonNull Object key) {
        if (cacheManager != null && cacheName != null && key != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Evicted entry {} from cache: {}", key, cacheName);
            }
        }
    }
}
