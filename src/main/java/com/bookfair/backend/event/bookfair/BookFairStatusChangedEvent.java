package com.bookfair.backend.event.bookfair;

import java.util.UUID;

public record BookFairStatusChangedEvent(UUID fairId, String oldStatus, String newStatus) {}
