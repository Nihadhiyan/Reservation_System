package com.bookfair.backend.event.cache;

import java.util.UUID;

/**
 * Event published when stall grid layouts are auto-generated or stall coordinates are modified.
 * Triggers asynchronous eviction of hallLayout and adminDashboard caches after transaction commit.
 */
public record LayoutUpdatedEvent(UUID hallId) {
}
