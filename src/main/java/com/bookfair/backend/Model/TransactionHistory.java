package com.bookfair.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transaction_histories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_role", nullable = false)
    private TransactionRole sourceRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_role", nullable = false)
    private TransactionRole destinationRole;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // Optional link to reservation if applicable

    public enum TransactionRole {
        VENDOR, ORGANIZER, VENUE_OWNER, PLATFORM
    }
}
