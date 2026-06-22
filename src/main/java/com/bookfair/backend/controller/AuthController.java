package com.bookfair.backend.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.auth.request.LoginRequest;
import com.bookfair.backend.dto.auth.request.RefreshTokenRequest;
import com.bookfair.backend.dto.auth.request.RegisterRequest;
import com.bookfair.backend.dto.auth.request.ResetPasswordRequest;
import com.bookfair.backend.dto.auth.request.VerifyEmailRequest;
import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.dto.common.ApiResponseDto;
import com.bookfair.backend.dto.user.request.ChangePasswordRequest;
import com.bookfair.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse data = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDto<>(true, "Registration successful", data, LocalDateTime.now()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse data = authService.login(loginRequest);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Login successful", data, LocalDateTime.now()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Successfully logged out", null, LocalDateTime.now()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponseDto<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse data = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Token refreshed successfully", data, LocalDateTime.now()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto<Void>> forgotPassword(@RequestParam("email") String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(new ApiResponseDto<>(true,
                "If an account with that email exists, a reset link has been sent.", null, LocalDateTime.now()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Password has been successfully reset.", null, LocalDateTime.now()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponseDto<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity
                .ok(new ApiResponseDto<>(true, "Password successfully updated.", null, LocalDateTime.now()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDto<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest verifyEmailRequest) {
        authService.verifyEmail(verifyEmailRequest);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Email successfully verified. You may now log in.", null,
                LocalDateTime.now()));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponseDto<Void>> sendVerification(@RequestParam("email") String email) {
        authService.sendVerificationEmail(email);
        return ResponseEntity.ok(new ApiResponseDto<>(true, "Verification email sent.", null, LocalDateTime.now()));
    }
}
