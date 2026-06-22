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
}
