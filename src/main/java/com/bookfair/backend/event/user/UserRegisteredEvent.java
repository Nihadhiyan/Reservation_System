package com.bookfair.backend.event.user;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String username, String email) implements UserEvent {
}
