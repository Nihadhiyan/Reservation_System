package com.bookfair.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.stall.request.CreateStallRequest;
import com.bookfair.backend.dto.stall.request.UpdateStallRequest;
import com.bookfair.backend.dto.stall.response.StallResponse;
import com.bookfair.backend.service.StallService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stalls")
@RequiredArgsConstructor
public class StallController {

    private final StallService stallService;

    @GetMapping("/hall/{hallId}")
    @PreAuthorize("hasAnyRole('USER', 'ORG_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<StallResponse>> getStallsByHall(@PathVariable UUID hallId) {
        return ResponseEntity.ok(stallService.getAllStallsForHall(hallId));
    }

    @GetMapping("/{stallId}")
    @PreAuthorize("hasAnyRole('USER', 'ORG_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<StallResponse> getStallById(@PathVariable UUID stallId) {
        return ResponseEntity.ok(stallService.getStallById(stallId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<List<StallResponse>> createStalls(@Valid @RequestBody List<CreateStallRequest> requests, Authentication authentication) {
        String currentUser = (authentication != null) ? authentication.getName() : "system";
        List<StallResponse> createdStalls = stallService.createStalls(requests, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStalls);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<StallResponse> updateStall(@PathVariable UUID id, @Valid @RequestBody UpdateStallRequest request) {
        return ResponseEntity.ok(stallService.updateStall(id, request));
    }

    @PatchMapping("/{stallId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<StallResponse> updateStallStatus(
            @PathVariable UUID stallId, 
            @RequestParam String status) {
        return ResponseEntity.ok(stallService.updateStallStatus(stallId, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateStall(@PathVariable UUID id) {
        stallService.deactivateStall(List.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<StallResponse>> getAvailableStalls() {
        return ResponseEntity.ok(stallService.getAvailableStalls());
    }
}
