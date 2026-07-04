package com.bookfair.backend.dto.organization.response;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Implements Serializable to ensure Redis cache compatibility
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationResponse implements Serializable {
    private UUID id;
    private String name;
    private String contactNumber;
    private String contactEmail;
    private String billingAddress;
    private Set<String> capabilities;
    private Boolean active;
}