package com.bookfair.backend.event.cache;

import java.util.UUID;

/**
 * Event published when stalls assigned to an event are created, updated, or removed.
 * Triggers asynchronous eviction of eventStalls cache after transaction commit.
 */
public record EventStallUpdatedEvent(UUID eventId) {
}
