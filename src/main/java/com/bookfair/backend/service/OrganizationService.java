package com.bookfair.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

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
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.DeletionAudit;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAllByActiveTrue(pageable)
                .map(organizationMapper::toOrganizationResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found", ErrorCode.ORGANIZATION_NOT_FOUND));

        return organizationMapper.toOrganizationResponse(organization);
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        if (organizationRepository.existsByNameAndActiveTrue(request.getName())) {
            throw new DuplicateResourceException("An organization with this name already exists.", ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        Organization organization = organizationMapper.toOrganizationFromCreateOrganizationRequest(request);
        Organization savedOrganization = organizationRepository.save(organization);

        return organizationMapper.toOrganizationResponse(savedOrganization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found", ErrorCode.ORGANIZATION_NOT_FOUND));

        // Check if they are trying to rename to a name that already exists (and isn't their own)
        if (!organization.getName().equalsIgnoreCase(request.getName()) && 
            organizationRepository.existsByNameAndActiveTrue(request.getName())) {
            throw new DuplicateResourceException("An organization with this name already exists.", ErrorCode.DUPLICATE_ORGANIZATION_NAME);
        }

        organizationMapper.updateOrganizationFromOrganizationRequest(request, organization);
        Organization updatedOrganization = organizationRepository.save(organization);

        return organizationMapper.toOrganizationResponse(updatedOrganization);
    }

    @Transactional
    public void deactivateOrganization(UUID id) {
        Organization organization = organizationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found", ErrorCode.ORGANIZATION_NOT_FOUND));

        organization.setActive(false);
        organization.setDeletionAudit(new DeletionAudit(LocalDateTime.now(), getCurrentUserId()));
        
        organizationRepository.save(organization);

        // Note: In a fully fleshed-out system, you might also want to publish an event here 
        // to deactivate all Users associated with this organization.
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }
}