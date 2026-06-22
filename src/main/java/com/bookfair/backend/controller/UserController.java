package com.bookfair.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.common.ApiResponseDto;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.user.request.UpdateUserRequest;
import com.bookfair.backend.dto.user.request.UpdateUserRoleRequest;
import com.bookfair.backend.dto.user.response.UserResponse;
import com.bookfair.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<UserResponse>>> getAllUsers(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> data = userService.getAllUsers(pageable);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Users retrieved successfully", data, LocalDateTime.now()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse data = userService.getUserProfile(id);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "User retrieved successfully", data, LocalDateTime.now()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponseDto<Void>> updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateUserRoleRequest updateUserRoleRequest) {
        userService.setRole(id, updateUserRoleRequest);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Role Updated Successfully", null, LocalDateTime.now()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteUserAsAdmin(@PathVariable UUID id) {
        userService.deleteUserAsAdmin(id);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "User deleted successfully", null, LocalDateTime.now()));
    }

    @PreAuthorize("isAuthenticated()")  
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteMyAccount(Authentication authentication) {
        String currentUserName = authentication.getName();
        userService.deleteMyAccount(currentUserName);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Account deleted successfully", null, LocalDateTime.now()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/reservations")
    public ResponseEntity<ApiResponseDto<List<ReservationResponse>>> getAllReservations(@PathVariable UUID id) {
        List<ReservationResponse> data = userService.getUserReservations(id);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "User reservations retrieved successfully", data, LocalDateTime.now()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserResponse>> getMyProfile(Authentication authentication) {
        String currentUsername = authentication.getName(); 
        UserResponse data = userService.getMyProfile(currentUsername);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Profile retrieved successfully", data, LocalDateTime.now()));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<ApiResponseDto<UserResponse>> updateUser(Authentication authentication, @Valid @RequestBody UpdateUserRequest userUpdateRequest) {
        String currentUsername = authentication.getName();
        UserResponse data = userService.updateUser(currentUsername, userUpdateRequest);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Profile updated successfully", data, LocalDateTime.now()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/reservations")
    public ResponseEntity<ApiResponseDto<List<ReservationResponse>>> getMyReservations(Authentication authentication) {
        List<ReservationResponse> data = userService.getMyReservations(authentication.getName());
        return ResponseEntity.ok(new ApiResponseDto<>(true, "My reservations retrieved successfully", data, LocalDateTime.now()));
    }
}
