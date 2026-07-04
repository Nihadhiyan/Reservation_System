package com.bookfair.backend.dto.pricing.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleResponse implements Serializable {
    private UUID id;
    private String name;
    private String description;
    private com.bookfair.backend.model.PricingRule.ConditionType conditionType;
    private String conditionValue;
    private BigDecimal multiplier;
    private Boolean active;
}
