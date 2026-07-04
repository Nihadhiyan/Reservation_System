package com.bookfair.backend.dto.auth.request;

import java.util.Set;

import com.bookfair.backend.model.Organization.OrganizationCapability;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String password;

    private boolean registerAsOrgAdmin;

    private String organizationName;

    private Set<OrganizationCapability> organizationCapabilities;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid contact number format")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String contactNumber;
    
    @NotBlank(message = "Address is required")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String address;
}
