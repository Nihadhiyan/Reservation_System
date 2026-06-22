package com.bookfair.backend.dto.organization.request;

import java.util.Set;
import com.bookfair.backend.model.Organization.OrganizationCapability;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid contact number format")
    private String contactNumber;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String contactEmail;

    @NotBlank(message = "Address is required")
    private String billingAddress;

    @NotEmpty(message = "At least one capability is required")
    private Set<OrganizationCapability> capabilities;
}
