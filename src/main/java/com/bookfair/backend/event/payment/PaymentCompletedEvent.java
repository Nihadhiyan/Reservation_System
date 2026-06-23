package com.bookfair.backend.event.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletedEvent(UUID reservationId, String transactionId, BigDecimal amount) {}
