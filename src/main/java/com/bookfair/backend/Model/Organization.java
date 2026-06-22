package com.bookfair.backend.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "organizations",
    indexes = {
        @Index(name = "idx_organization_active", columnList = "active"),
    }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class Organization extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    @NotBlank
    private String name;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "billing_address")
    private String billingAddress;
 
    @ElementCollection(targetClass = OrganizationCapability.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "organization_capabilities",
        joinColumns = @JoinColumn(name = "organization_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false)
    private Set<OrganizationCapability> capabilities;

    @OneToMany(mappedBy = "organization")
    private List<User> employees;

    @OneToMany(mappedBy = "owner")
    private List<Venue> ownedVenues;

    @ManyToMany(mappedBy = "partners")
    private List<Venue> partnerVenues;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Embedded
    private DeletionAudit deletionAudit;

    public enum OrganizationCapability {
        OWNS_VENUES,
        ORGANIZES_EVENTS,
        OPERATES_STALLS
    }
}