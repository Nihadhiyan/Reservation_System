package com.bookfair.backend.model;

import java.util.List;
import java.util.UUID;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "venues",
    indexes = {
        @Index(name = "idx_venue_name", columnList = "name")
    }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Venue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Venue name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Column(name = "latitude", precision = 9, scale = 6)
    private Double latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private Double longitude;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Column(name = "total_square_footage")
    @Min(value = 0, message = "Total square footage must be non-negative")
    private Double totalSquareFootage;

    @Column(name = "parking_available")
    private Boolean parkingAvailable = false;

    @Column(name = "food_court_available")
    private Boolean foodCourtAvailable = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "venue_blueprint_image_url")
    private String blueprintImageUrl;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LayoutMarker> markers;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Building> buildings;

}
