package com.bookfair.backend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.auth.mapper.AuthMapper;
import com.bookfair.backend.dto.auth.request.LoginRequest;
import com.bookfair.backend.dto.auth.request.RefreshTokenRequest;
import com.bookfair.backend.dto.auth.request.RegisterRequest;
import com.bookfair.backend.dto.auth.request.ResetPasswordRequest;
import com.bookfair.backend.dto.auth.request.VerifyEmailRequest;
import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.dto.user.request.ChangePasswordRequest;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.CustomUserDetailsService;
import com.bookfair.backend.security.CustomUserPrincipal;
import com.bookfair.backend.security.JwtService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

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

        userDetailsService.evictUserDetails(savedUser);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        Long expiresIn = jwtService.getAccessTokenExpirationTime() / 1000; // 1 hour in seconds

        return authMapper.toAuthResponse(savedUser, accessToken, refreshToken, expiresIn);

    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        User user = userRepository.findByUsernameAndActiveTrue(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid username or password", ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Long expiresIn = jwtService.getAccessTokenExpirationTime() / 1000; // 1 hour in seconds

        return authMapper.toAuthResponse(user, accessToken, refreshToken, expiresIn);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken (RefreshTokenRequest refreshTokenRequest) {
        UUID userId = jwtService.extractUserId(refreshTokenRequest.getRefreshToken());

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (userId != null && jwtService.validateToken(refreshTokenRequest.getRefreshToken(), userDetails)) {
            User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            Long expiresIn = jwtService.getAccessTokenExpirationTime() / 1000;

            return authMapper.toAuthResponse(user, newAccessToken, newRefreshToken, expiresIn);

        }

        throw new BusinessException("Invalid refresh token", ErrorCode.UNAUTHORIZED);
    }

    public void logout(String authHeader) {
        // Future enhancement: Add token to a Redis blacklist database here
        log.info("User requested logout. Frontend should clear tokens.");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
       
        long remainingTime = jwtService.getRemainingExpirationTime(token);

        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(
                "blacklist:" + token, 
                "revoked", 
                remainingTime, 
                TimeUnit.MILLISECONDS
            );

            log.info("Token successfully blacklisted in Redis.");
        }
    }

    public void forgotPassword(String email) {
        userRepository.findByEmailAndActiveTrue(email).ifPresent(user -> {
            String resetToken = jwtService.generatePasswordResetToken(user);

            String resetLink = "https://clausis.com/reset-password?token=" + resetToken;

            Map<String, Object> emailVariables = new HashMap<>();
            emailVariables.put("userName", user.getUsername());
            emailVariables.put("resetLink", resetLink);

            emailService.sendEmail(
                user.getEmail(), 
                "Password Reset Request", 
                "password_reset_template", 
                emailVariables, 
                null
            );

        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = jwtService.extractUserId(request.getResetToken());

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (userId == null || !jwtService.validateToken(request.getResetToken(), userDetails)) {
            throw new BusinessException("Invalid or expired reset token.", ErrorCode.UNAUTHORIZED);
        }

        String tokenPurpose = jwtService.extractPurpose(request.getResetToken());

        if (!"RESET_PASSWORD".equals(tokenPurpose)) {
            throw new BusinessException("Invalid token.", ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        userDetailsService.evictUserDetails(user);
        

        Map<String, Object> emailVariables = new HashMap<>();
        emailVariables.put("userName", user.getUsername());
        
        emailService.sendEmail(
            user.getEmail(), 
            "Your Password Has Been Changed", 
            "password_reset_success_template", 
            emailVariables, 
            null
        );
    }

    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest){
        UUID currentUserId = getCurrentUserId();
        
        User user = userRepository.findByIdAndActiveTrue(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect current password", ErrorCode.UNAUTHORIZED);
        }

        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password cannot be the same as the old password", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);

        userDetailsService.evictUserDetails(user);

    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest verifyEmailRequest) {

        String verificationToken = verifyEmailRequest.getToken();

        UUID userId = jwtService.extractUserId(verificationToken);

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (userId == null || !jwtService.validateToken(verificationToken, userDetails)) {
            throw new BusinessException("Invalid or expired verification token.", ErrorCode.UNAUTHORIZED);
        }

        String tokenPurpose = jwtService.extractPurpose(verificationToken);

        if (!"VERIFY_EMAIL".equals(tokenPurpose)) {
            throw new BusinessException("Invalid token.", ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        user.setEmailVerified(true);
        userRepository.save(user);

        userDetailsService.evictUserDetails(user);
    }

    public void sendVerificationEmail (String email) {
        userRepository.findByEmailAndActiveTrue(email).ifPresent(user -> {
            String verificationToken = jwtService.generateVerificationToken(user);

            String verificationLink = "https://clausis.com/verify-email?token=" + verificationToken;

            Map<String, Object> emailVariables = new HashMap<>();
            emailVariables.put("userName", user.getUsername());
            emailVariables.put("verificationLink", verificationLink);

            emailService.sendEmail(
                user.getEmail(), 
                "Verify Your Email", 
                "email_verification_template", 
                emailVariables, 
                null
            );

        });
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }
}
