package com.bookfair.backend.security;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.bookfair.backend.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;

@Component("orgAuth")
@RequiredArgsConstructor
public class OrganizationSecurityEvaluator {

    private final OrganizationRepository organizationRepository;

    /**
     * Checks if the current user is an Admin of an ORGANIZER type organization.
     */
    public boolean isOrganizerAdmin(Authentication authentication, UUID orgId) {
        return checkPermission(authentication, orgId, "ORGANIZER", "ORG_ADMIN");
    }

    /**
     * Checks if the current user is an Admin of a VENDOR type organization.
     */
    public boolean isVendorAdmin(Authentication authentication, UUID orgId) {
        return checkPermission(authentication, orgId, "VENDOR", "ORG_ADMIN");
    }

    /**
     * Checks if the user is AT LEAST a member of the specific organization,
     * regardless of type (Useful for generic view endpoints).
     */
    public boolean isMemberOf(Authentication authentication, UUID orgId) {
        Objects.requireNonNull(orgId, "Organization ID cannot be null");
        Map<String, String> orgRoles = extractOrgRoles(authentication);
        return orgRoles.containsKey(orgId.toString());
    }

    // --- Internal Helper Methods ---

    private boolean checkPermission(Authentication authentication, UUID orgId, String requiredType,
            String requiredRole) {
        Objects.requireNonNull(orgId, "Organization ID cannot be null");

        Map<String, String> orgRoles = extractOrgRoles(authentication);
        String orgIdStr = orgId.toString();

        if (!orgRoles.containsKey(orgIdStr)) {
            return false; // Not a member of this org at all
        }

        String actualRole = orgRoles.get(orgIdStr);
        if (!actualRole.equalsIgnoreCase(requiredRole)) {
            return false;
        }

        return organizationRepository.findById(orgId)
                .map(org -> switch (requiredType) {
                    case "ORGANIZER" -> org.isEventOrganizer();
                    case "VENDOR" -> org.isVendor();
                    default -> true;
                })
                .orElse(false);
    }

    private Map<String, String> extractOrgRoles(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return Map.of(); // Empty map if not authenticated properly
        }

        Map<String, String> orgRoles = principal.getOrgRoles();
        return orgRoles != null ? orgRoles : Map.of();
    }
}