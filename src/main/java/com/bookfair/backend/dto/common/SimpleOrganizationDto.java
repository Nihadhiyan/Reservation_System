package com.bookfair.backend.dto.common;

import java.util.Set;
import java.util.UUID;

import com.bookfair.backend.model.Organization.OrganizationCapability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleOrganizationDto {
    private UUID id;
    private String name;
    private Set<OrganizationCapability> capabilities;
}