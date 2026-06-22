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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "reservations",
    indexes = {
        @Index(name = "idx_reservation_user", columnList = "user_id"),
        @Index(name = "idx_reservation_event", columnList = "event_id"),
        @Index(name = "idx_reservation_expires", columnList = "expires_at"),
        @Index(name = "idx_reservation_status", columnList = "status")
    }
)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Event event;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReservationStall> reservedStalls;

    @Column(name = "reservation_start_time", nullable = false)
    private LocalDateTime reservationStartDateTime;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Genre genre;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    @Positive(message = "Total price must be positive")
    private BigDecimal totalPrice;

    @Column(name = "qr_code_payload", columnDefinition = "TEXT")
    private String qrCodePayload; // Stores the JWT String for the scanner app

    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, REJECTED, REFUNDED, REFUND_PENDING
    }
}
