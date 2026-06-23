package com.bookfair.backend.event.user;

import java.util.UUID;

public record PasswordResetRequestedEvent(UUID userId, String resetLink) implements UserEvent {
}
