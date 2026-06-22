package com.bookfair.backend.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.common.ApiResponseDto;
import com.bookfair.backend.dto.organization.request.CreateOrganizationRequest;
import com.bookfair.backend.dto.organization.request.UpdateOrganizationRequest;
import com.bookfair.backend.dto.organization.response.OrganizationResponse;
import com.bookfair.backend.service.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponseDto<OrganizationResponse>> createOrganization(@RequestBody @Valid CreateOrganizationRequest request) {
        OrganizationResponse response = organizationService.createOrganization(request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Organization created successfully", response, LocalDateTime.now()));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<OrganizationResponse>>> getAllOrganizations(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<OrganizationResponse> response = organizationService.getAllOrganizations(pageable);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Organizations retrieved successfully", response, LocalDateTime.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrganizationResponse>> getOrganization(@PathVariable UUID id) {
        OrganizationResponse response = organizationService.getOrganizationById(id);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Organization retrieved successfully", response, LocalDateTime.now()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrganizationResponse>> updateOrganization(@PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationResponse response = organizationService.updateOrganization(id, request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Organization updated successfully", response, LocalDateTime.now()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteOrganization(@PathVariable UUID id) {
        organizationService.deactivateOrganization(id);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Organization deleted successfully", null, LocalDateTime.now()));
    }
}
