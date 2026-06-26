package com.bookfair.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.organization.mapper.OrganizationMapper;
import com.bookfair.backend.dto.organization.request.CreateOrganizationRequest;
import com.bookfair.backend.dto.organization.request.UpdateOrganizationRequest;
import com.bookfair.backend.dto.organization.response.OrganizationResponse;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ForbiddenException;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.DeletionAudit;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.SystemRole;
import com.bookfair.backend.model.OrganizationRole;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.repository.OrganizationMemberRepository;
import com.bookfair.backend.security.CustomUserPrincipal;
import com.bookfair.backend.event.organization.OrganizationDeactivatedEvent;
import com.bookfair.backend.event.organization.OrganizationCapabilityChangedEvent;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationMapper organizationMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getAllOrganizations(Pageable pageable) {
        requireNonNull(pageable, "pageable cannot be null");
        return organizationRepository.findAllByActiveTrue(pageable)
                .map(organizationMapper::toOrganizationResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        return organizationMapper.toOrganizationResponse(organization);
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        if (organizationRepository.existsByNameAndActiveTrue(request.getName())) {
            throw new DuplicateResourceException("An organization with this name already exists.",
                    ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        Organization organization = organizationMapper.toOrganizationFromCreateOrganizationRequest(request);
        Organization savedOrganization = organizationRepository.save(organization);

        return organizationMapper.toOrganizationResponse(savedOrganization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        User requestingUser = getCurrentUser();

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), organization.getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot modify organizations outside your context.", ErrorCode.FORBIDDEN);
            }
        }

        // Check if they are trying to rename to a name that already exists (and isn't
        // their own)
        if (!organization.getName().equalsIgnoreCase(request.getName()) &&
                organizationRepository.existsByNameAndActiveTrue(request.getName())) {
            throw new DuplicateResourceException("An organization with this name already exists.",
                    ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        java.util.Set<com.bookfair.backend.model.Organization.OrganizationCapability> oldCapabilities = new java.util.HashSet<>(
                organization.getCapabilities());

        organizationMapper.updateOrganizationFromOrganizationRequest(request, organization);
        Organization updatedOrganization = organizationRepository.save(organization);

        // We check for equality before publishing to avoid unnecessary events.
        // Firing events on every update (even when capabilities didn't change) creates
        // unnecessary processing overhead
        // and could trigger unintended side effects.
        if (!oldCapabilities.equals(organization.getCapabilities())) {
            applicationEventPublisher.publishEvent(
                    new OrganizationCapabilityChangedEvent(
                            organization.getId(),
                            organization.getCapabilities()));
        }

        return organizationMapper.toOrganizationResponse(updatedOrganization);
    }

    @Transactional
    public void deactivateOrganization(UUID id) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        User requestingUser = getCurrentUser();

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), organization.getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot deactivate organizations outside your context.", ErrorCode.FORBIDDEN);
            }
        }

        softDelete(organization);

        publishOrganizationDeactivatedEvent(organization.getId());
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }

    // Centralizing current-user retrieval improves maintainability by eliminating
    // duplicate code
    // across the service methods. It also ensures consistent error handling if the
    // current user cannot be found.
    private User getCurrentUser() {
        return userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));
    }

    // Soft-delete logic is extracted to a single place to ensure consistency.
    // If the audit details or active flag logic change in the future, only this
    // method needs updating.
    private void softDelete(Organization organization) {
        organization.setActive(false);
        organization.setDeletionAudit(new DeletionAudit(LocalDateTime.now(), getCurrentUserId()));
        organizationRepository.save(organization);
    }

    // Events are used to avoid direct service coupling. This allows other parts of
    // the system
    // (like listeners) to react to organization deactivation without tightly
    // coupling the
    // OrganizationService to User or Cache services.
    private void publishOrganizationDeactivatedEvent(UUID organizationId) {
        applicationEventPublisher.publishEvent(
                new OrganizationDeactivatedEvent(organizationId, getCurrentUserId()));
    }
}
