package com.bookfair.backend.event;

import java.util.UUID;

/**
 * Event indicating that a user has been updated.
 * 
 * <p>
 * <strong>When it is published:</strong> Published by UserService when a user
 * update
 * request contains changes that affect the user's properties.
 * </p>
 * 
 * <p>
 * <strong>Who consumes it:</strong> Consumed by UserEventListener to update
 * the corresponding user in the database.
 * </p>
 * 
 * <p>
 * <strong>Why it exists:</strong> It ensures that user records remain
 * synchronized with their
 * organization's capabilities without directly coupling the UserService to
 * UserRepository operations.
 * </p>
 * 
 * <p>
 * <strong>What the payload represents:</strong> The unique identifier of the
 * user and the updated set of its capabilities.
 * </p>
 */
public record UserUpdatedEvent(
        UUID userId,
        String username) implements UserEvent {
}
