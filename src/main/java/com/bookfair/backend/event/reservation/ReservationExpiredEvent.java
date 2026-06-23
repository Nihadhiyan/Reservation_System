package com.bookfair.backend.event.reservation;

import java.util.UUID;
import com.bookfair.backend.event.user.UserEvent;

public record ReservationExpiredEvent(UUID userId, String eventName) implements UserEvent {
}
