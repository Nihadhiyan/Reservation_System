package com.bookfair.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "event_settlements")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventSettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organization organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_owner_id", nullable = false)
    private Organization venueOwner;

    // Snapshotted fields
    @Column(name = "snapshotted_daily_rent_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal snapshottedDailyRentRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshotted_rent_type", nullable = false)
    private Venue.RentType snapshottedRentType;

    // Financial tracking
    @Column(name = "total_rent_owed", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalRentOwed = BigDecimal.ZERO;

    @Column(name = "amount_paid_to_owner", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaidToOwner = BigDecimal.ZERO;

    @Column(name = "remaining_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal remainingBalance = BigDecimal.ZERO;

    @Column(name = "organizer_profit", precision = 10, scale = 2, nullable = false)
    private BigDecimal organizerProfit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status = SettlementStatus.LIABILITY_PENDING;

    public enum SettlementStatus {
        LIABILITY_PENDING, LIABILITY_COVERED, PROFIT_RELEASED
    }
}
