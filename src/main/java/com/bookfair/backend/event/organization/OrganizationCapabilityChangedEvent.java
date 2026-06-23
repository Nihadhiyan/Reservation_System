package com.bookfair.backend.event;

import java.util.Set;
import java.util.UUID;

import com.bookfair.backend.model.Organization.OrganizationCapability;

/**
 * Event indicating that the capabilities of an organization have been changed.
 * 
 * <p><strong>When it is published:</strong> Published by OrganizationService when an organization update
 * request contains capabilities that are different from the organization's existing capabilities.</p>
 * 
 * <p><strong>Who consumes it:</strong> Consumed by OrganizationEventListener to update the
 * corresponding capabilities for all users associated with this organization in the database.</p>
 * 
 * <p><strong>Why it exists:</strong> It ensures that user records remain synchronized with their 
 * organization's capabilities without directly coupling the OrganizationService to UserRepository operations.</p>
 * 
 * <p><strong>What the payload represents:</strong> The unique identifier of the organization and the updated set of its capabilities.</p>
 */
public record OrganizationCapabilityChangedEvent(
        UUID organizationId,
        Set<OrganizationCapability> newCapability) implements OrganizationEvent {
}
