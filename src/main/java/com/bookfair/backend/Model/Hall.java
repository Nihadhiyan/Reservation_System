package com.bookfair.backend.model;

import java.util.List;
import java.util.UUID;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "halls", indexes = {
        @Index(name = "idx_hall_floor", columnList = "floor_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_hall_floor_name", columnNames = { "floor_id", "name" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Hall extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Hall name is required")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_category", nullable = false)
    private SpaceCategory spaceCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_type", nullable = false)
    private HallType hallType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Floor floor;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Stall> stalls;

    @Column(name = "blueprint_image_url")
    private String blueprintImageUrl;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "xCoord", column = @Column(name = "hall_x_coord")),
            @AttributeOverride(name = "yCoord", column = @Column(name = "hall_y_coord")),
            @AttributeOverride(name = "width", column = @Column(name = "hall_width")),
            @AttributeOverride(name = "height", column = @Column(name = "hall_height"))
    })
    private LayoutPosition layout;

    @Column(name = "square_footage")
    @Min(value = 0, message = "Square footage must be non-negative")
    private Double squareFootage;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "max_stalls")
    @Min(value = 0, message = "Max stalls must be non-negative")
    private Integer maxStalls;

    @Column(name = "wifi_available")
    private Boolean wifiAvailable = false;

    @Column(name = "air_conditioned")
    private Boolean airConditioned = false;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LayoutMarker> markers;

    public enum SpaceCategory {
        INDOOR,
        OUTDOOR
    }

    public enum HallType {
        STANDARD,
        PREMIUM,
        FOOD_COURT,
        CHILDREN,
        VIP,
        SPONSOR,
        EXHIBITION,
        GENERAL
    }

}
