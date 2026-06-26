package com.bookfair.backend.repository;

import com.bookfair.backend.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    List<OrganizationMember> findByUserId(UUID userId);

    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    List<OrganizationMember> findByOrganizationIdAndActiveTrue(UUID organizationId);
}
