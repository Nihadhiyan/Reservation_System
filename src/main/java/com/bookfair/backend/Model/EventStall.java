package com.bookfair.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "event_stalls", indexes = {
        @Index(name = "idx_es_event", columnList = "event_id"),
        @Index(name = "idx_es_stall", columnList = "stall_id"),
        @Index(name = "idx_es_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "event_id", "stall_id" }, name = "uk_event_stall")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventStall extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Stall stall;

    @Column(name = "stall_name_at_creation")
    private String stallNameAtCreation;

    @Column(name = "hall_id_snapshot")
    private UUID hallIdSnapshot;

    @Column(name = "hall_name_at_creation")
    private String hallNameAtCreation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "xCoord", column = @Column(name = "event_stall_x_coord")),
            @AttributeOverride(name = "yCoord", column = @Column(name = "event_stall_y_coord")),
            @AttributeOverride(name = "width", column = @Column(name = "event_stall_width")),
            @AttributeOverride(name = "height", column = @Column(name = "event_stall_height"))
    })
    private LayoutPosition layout;

    @Column(nullable = false, precision = 10, scale = 2)
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    @Positive(message = "Manual override price must be positive")
    private BigDecimal manualOverridePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvailabilityStatus status;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public enum AvailabilityStatus {
        AVAILABLE, BOOKED, BLOCKED
    }
}
