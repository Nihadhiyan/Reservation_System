package com.bookfair.backend.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "reservation_stalls",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"reservation_id", "book_fair_stall_id"},
            name = "uk_reservation_stall"
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStall extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_fair_stall_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BookFairStall bookFairStall;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    @Positive(message = "Price at booking must be positive")
    private BigDecimal priceAtBooking;
}
