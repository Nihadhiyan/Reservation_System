package com.bookfair.backend.dto.common;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.bookfair.backend.model.Organization.OrganizationCapability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Implements Serializable for Redis caching compatibility
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleOrganizationDto implements Serializable {
    private UUID id;
    private String name;
    private Set<OrganizationCapability> capabilities;
}