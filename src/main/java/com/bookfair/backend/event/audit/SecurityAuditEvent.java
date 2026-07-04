package com.bookfair.backend.event.audit;

import java.time.Instant;

public record SecurityAuditEvent(String action, String performedBy, String details, Instant timestamp) {
}
