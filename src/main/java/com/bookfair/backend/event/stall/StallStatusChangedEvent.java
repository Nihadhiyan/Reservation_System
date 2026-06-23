package com.bookfair.backend.event.stall;

import java.util.UUID;

public record StallStatusChangedEvent(UUID stallId, String stallNumber, String oldStatus, String newStatus) {}
