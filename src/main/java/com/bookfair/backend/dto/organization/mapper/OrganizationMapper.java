package com.bookfair.backend.dto.organization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.common.SimpleOrganizationDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.organization.request.CreateOrganizationRequest;
import com.bookfair.backend.dto.organization.request.UpdateOrganizationRequest;
import com.bookfair.backend.dto.organization.response.OrganizationResponse;
import com.bookfair.backend.model.Organization;

@Mapper(config = GlobalMapperConfig.class)
public interface OrganizationMapper {
    OrganizationResponse toOrganizationResponse(Organization organization);

    SimpleOrganizationDto toSimpleOrganizationDto(Organization organization);

    Organization toOrganizationFromCreateOrganizationRequest(CreateOrganizationRequest request);

    Organization updateOrganizationFromOrganizationRequest(UpdateOrganizationRequest request, @MappingTarget Organization organization);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "active", constant = "true")
    @org.mapstruct.Mapping(target = "name", source = "organizationName")
    @org.mapstruct.Mapping(target = "capabilities", source = "organizationCapabilities")
    @org.mapstruct.Mapping(target = "contactNumber", source = "contactNumber")
    @org.mapstruct.Mapping(target = "billingAddress", source = "address")
    @org.mapstruct.Mapping(target = "contactEmail", source = "email")
    Organization toOrganizationFromRegisterRequest(com.bookfair.backend.dto.auth.request.RegisterRequest registerRequest);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "user", source = "user")
    @org.mapstruct.Mapping(target = "organization", source = "organization")
    @org.mapstruct.Mapping(target = "role", source = "role")
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "active", ignore = true)
    com.bookfair.backend.model.OrganizationMember toOrganizationMember(com.bookfair.backend.model.User user, Organization organization, com.bookfair.backend.model.OrganizationMember.OrganizationRole role);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "used", constant = "false")
    @org.mapstruct.Mapping(target = "organizationId", source = "organizationId")
    @org.mapstruct.Mapping(target = "email", source = "request.email")
    @org.mapstruct.Mapping(target = "assignedRole", source = "request.role")
    @org.mapstruct.Mapping(target = "token", source = "token")
    @org.mapstruct.Mapping(target = "expiresAt", source = "expiresAt")
    com.bookfair.backend.model.OrganizationInvite toOrganizationInvite(java.util.UUID organizationId, com.bookfair.backend.dto.organization.request.InviteRequest request, String token, java.time.Instant expiresAt);
}

