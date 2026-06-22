package com.bookfair.backend.dto.organization.response;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String contactNumber;
    private String contactEmail;
    private String billingAddress;
    private Set<String> capabilities;
    private Boolean active;
}