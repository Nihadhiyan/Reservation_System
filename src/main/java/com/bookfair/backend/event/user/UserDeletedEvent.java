package com.bookfair.backend.event.user;

import java.util.UUID;

public record UserDeletedEvent(UUID userId, String username, String email) implements UserEvent {
}
