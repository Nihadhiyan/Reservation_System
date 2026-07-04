package com.bookfair.backend.event.cache;

import java.util.UUID;

/**
 * Event published when an organization is created, updated, or deactivated.
 * Used by CacheEvictionListener to asynchronously evict organization, event, and user profile caches after transaction commit.
 */
public record OrganizationUpdatedEvent(UUID organizationId) {
}
