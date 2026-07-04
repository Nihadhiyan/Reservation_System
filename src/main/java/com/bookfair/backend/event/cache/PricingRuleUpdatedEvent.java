package com.bookfair.backend.event.cache;

import java.util.UUID;

public record PricingRuleUpdatedEvent(UUID ruleId) {
}
