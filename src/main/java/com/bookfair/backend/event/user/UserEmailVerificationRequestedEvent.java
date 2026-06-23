package com.bookfair.backend.event.user;

import java.util.UUID;

public record UserEmailVerificationRequestedEvent(UUID userId, String verificationLink) implements UserEvent {
}
