package com.bookfair.backend.event.reservation;

import java.util.UUID;
import com.bookfair.backend.event.user.UserEvent;

public record ReservationConfirmedEvent(UUID userId, String eventName, String qrCodeBase64) implements UserEvent {
}
