package com.bookfair.backend.event.cache;

import java.util.UUID;

/**
 * Event published when an event is created, updated, deleted, or changes status.
 * Triggers asynchronous eviction of event and event stall caches after transaction commit.
 */
public record EventUpdatedEvent(UUID eventId) {
}
