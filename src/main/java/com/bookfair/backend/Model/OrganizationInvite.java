package com.bookfair.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import com.bookfair.backend.model.OrganizationMember.OrganizationRole;

@Entity
@Table(name = "organization_invites", indexes = {
        @Index(name = "idx_org_invite_token", columnList = "token"),
        @Index(name = "idx_org_invite_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    private OrganizationRole assignedRole;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean used = false;
}
