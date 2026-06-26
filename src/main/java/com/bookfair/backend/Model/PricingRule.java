package com.bookfair.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PricingRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionType conditionType; // e.g., ORG_TYPE, DURATION, SEASONAL

    @Column(nullable = false)
    private String conditionValue; // e.g., NON_PROFIT, >7_DAYS, SUMMER

    @Column(nullable = false)
    private BigDecimal multiplier;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "priority")
    private Integer priority;

    public enum ConditionType {
        ORG_TYPE, DURATION, SEASONAL
    }

}
