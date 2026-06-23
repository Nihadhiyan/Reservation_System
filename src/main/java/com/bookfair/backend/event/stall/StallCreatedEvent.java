package com.bookfair.backend.event.stall;

import java.util.UUID;

public record StallCreatedEvent(UUID stallId, String stallNumber, UUID hallId, String createdBy) {}
