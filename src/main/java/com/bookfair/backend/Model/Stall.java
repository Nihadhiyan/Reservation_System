package com.bookfair.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

// Implements Serializable explicitly for Redis caching compatibility
@Entity
@Table(name = "stalls", indexes = {
        @Index(name = "idx_stall_hall", columnList = "hall_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_stall_name_hall", columnNames = { "hall_id", "name" })
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Stall extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Stall name is required")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Hall hall;

    @Enumerated(EnumType.STRING)
    @Column(name = "stall_type")
    private StallType stallType;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "xCoord", column = @Column(name = "stall_x_coord")),
            @AttributeOverride(name = "yCoord", column = @Column(name = "stall_y_coord")),
            @AttributeOverride(name = "width", column = @Column(name = "stall_width")),
            @AttributeOverride(name = "height", column = @Column(name = "stall_height"))
    })
    private LayoutPosition layout;

    @Column(name = "square_footage")
    @Min(value = 0, message = "Square footage must be non-negative")
    private Double squareFootage;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public enum StallType {
        STANDARD,
        CORNER,
        ISLAND,
        SPONSOR,
        PREMIUM
    }
}