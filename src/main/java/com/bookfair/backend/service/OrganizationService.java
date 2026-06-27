package com.bookfair.backend.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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
import com.bookfair.backend.model.Organization.OrganizationCapability;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.SystemRole;
import com.bookfair.backend.model.OrganizationMember.OrganizationRole;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.repository.OrganizationMemberRepository;
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
        Organization organization = organizationRepository.findByIdAndActiveTrue(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        return organizationMapper.toOrganizationResponse(organization);
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        requireNonNull(request, "request cannot be null");
        if (organizationRepository.existsByNameAndActiveTrue(requireNonNull(request.getName()))) {
            throw new DuplicateResourceException("An organization with this name already exists.",
                    ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        Organization organization = organizationMapper.toOrganizationFromCreateOrganizationRequest(request);
        Organization savedOrganization = organizationRepository.save(requireNonNull(organization));

        return organizationMapper.toOrganizationResponse(savedOrganization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
        requireNonNull(request, "request cannot be null");
        Organization organization = organizationRepository.findByIdAndActiveTrue(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        User requestingUser = getCurrentUser();

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository
                    .findByUserIdAndOrganizationId(requestingUser.getId(), organization.getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot modify organizations outside your context.",
                        ErrorCode.FORBIDDEN);
            }
        }

        // rename conflict check (exclude self)
        if (!organization.getName().equalsIgnoreCase(request.getName()) &&
                organizationRepository.existsByNameAndActiveTrue(requireNonNull(request.getName()))) {
            throw new DuplicateResourceException("An organization with this name already exists.",
                    ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        Set<OrganizationCapability> oldCapabilities = new HashSet<>(organization.getCapabilities());

        organizationMapper.updateOrganizationFromOrganizationRequest(request, organization);
        Organization updatedOrganization = organizationRepository.save(organization);

        if (!oldCapabilities.equals(organization.getCapabilities())) {
            applicationEventPublisher.publishEvent(
                    new OrganizationCapabilityChangedEvent(
                            requireNonNull(organization.getId()),
                            requireNonNull(organization.getCapabilities())));
        }

        return organizationMapper.toOrganizationResponse(updatedOrganization);
    }

    @Transactional
    public void deactivateOrganization(UUID id) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        User requestingUser = getCurrentUser();

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository
                    .findByUserIdAndOrganizationId(requestingUser.getId(), organization.getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot deactivate organizations outside your context.",
                        ErrorCode.FORBIDDEN);
            }
        }

        softDelete(organization);

        publishOrganizationDeactivatedEvent(organization.getId());
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }

        if (authentication != null && authentication.getPrincipal() instanceof String userIdString) {
            return UUID.fromString(userIdString);
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }

    private User getCurrentUser() {
        return userRepository.findById(requireNonNull(getCurrentUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));
    }

    private void softDelete(Organization organization) {
        organization.setActive(false);
        organization.setDeletionAudit(new DeletionAudit(Instant.now(), requireNonNull(getCurrentUserId())));
        organizationRepository.save(organization);
    }

    private void publishOrganizationDeactivatedEvent(UUID organizationId) {
        applicationEventPublisher.publishEvent(
                new OrganizationDeactivatedEvent(requireNonNull(organizationId), requireNonNull(getCurrentUserId())));
    }
}
