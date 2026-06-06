package com.bookfair.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.auth.mapper.AuthMapper;
import com.bookfair.backend.dto.auth.request.LoginRequest;
import com.bookfair.backend.dto.auth.request.RegisterRequest;
import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.JwtService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {

        if (userRepository.existsByUsernameAndActiveTrue(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username is already taken", ErrorCode.DUPLICATE_USERNAME);
        }

        if (userRepository.existsByEmailAndActiveTrue(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email is already registered", ErrorCode.DUPLICATE_EMAIL);
        }

        User user = authMapper.toUserFromRegisterRequest(registerRequest);

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        Long expiresIn = 3600L; // 1 hour in seconds

        return authMapper.toAuthResponse(savedUser, accessToken, refreshToken, expiresIn);

    }

    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        User user = userRepository.findByUsernameAndActiveTrue(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid username or password", ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Long expiresIn = 3600L; // 1 hour in seconds

        return authMapper.toAuthResponse(user, accessToken, refreshToken, expiresIn);
    }
}
