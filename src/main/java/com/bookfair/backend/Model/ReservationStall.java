package com.bookfair.backend.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "reservation_stalls",
     indexes = {
        @Index(name = "idx_rs_reservation", columnList = "reservation_id"),
        @Index(name = "idx_rs_event_stall", columnList = "event_stall_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"reservation_id", "event_stall_id"},
            name = "uk_reservation_stall"
        )
    }
)
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
    @JoinColumn(name = "event_stall_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EventStall eventStall;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    @Positive(message = "Price at booking must be positive")
    private BigDecimal priceAtBooking;
}
