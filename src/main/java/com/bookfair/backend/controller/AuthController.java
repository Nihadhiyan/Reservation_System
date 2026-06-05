package com.bookfair.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookfair.backend.dto.auth.request.LoginRequest;
import com.bookfair.backend.dto.auth.request.RegisterRequest;
import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {

        return ResponseEntity.ok(authService.register(registerRequest));

    }

    @PostMapping("/login") 
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {

        return ResponseEntity.ok(authService.login(loginRequest));

    }
}
