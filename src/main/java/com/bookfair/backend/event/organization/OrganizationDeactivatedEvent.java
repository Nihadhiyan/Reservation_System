package com.bookfair.backend.event;

import java.util.UUID;

/**
 * Event indicating that an organization has been deactivated.
 * 
 * <p><strong>When it is published:</strong> Published by OrganizationService after successfully
 * performing a soft delete on an organization and committing it to the database.</p>
 * 
 * <p><strong>Who consumes it:</strong> Consumed by OrganizationEventListener to deactivate all 
 * associated users, and UserCacheListener to eventually trigger bulk cache eviction for those users.</p>
 * 
 * <p><strong>Why it exists:</strong> It decouples the organization deactivation logic from
 * downstream side effects like user deactivation and cache eviction, promoting a modular architecture.</p>
 * 
 * <p><strong>What the payload represents:</strong> The unique identifier of the organization that was deactivated.</p>
 */
public record OrganizationDeactivatedEvent(
        UUID organizationId) implements OrganizationEvent {
}
