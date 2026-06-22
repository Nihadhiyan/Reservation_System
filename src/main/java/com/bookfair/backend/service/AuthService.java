package com.bookfair.backend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
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
import com.bookfair.backend.event.UserUpdatedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.OrganizationRepository;
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
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {

        if (registerRequest.getRole() == User.Role.ORG_EMPLOYEE || registerRequest.getRole() == User.Role.SUPER_ADMIN) {
            throw new BusinessException(
                "Employees and Admins cannot register directly. Please ask your organization administrator to invite you.", 
                ErrorCode.FORBIDDEN
            );
        }

        if (userRepository.existsByUsernameAndActiveTrue(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username is already taken", ErrorCode.DUPLICATE_USERNAME);
        }

        if (userRepository.existsByEmailAndActiveTrue(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email is already registered", ErrorCode.DUPLICATE_EMAIL);
        }

        

        Organization savedOrganization = null;

        if (registerRequest.getRole() == User.Role.ORG_ADMIN) {
            if (registerRequest.getOrganizationName() == null || registerRequest.getOrganizationName().isBlank()) {
                throw new BusinessException("Organization name is required for business accounts.", ErrorCode.VALIDATION_ERROR);
            }

            if (organizationRepository.existsByNameAndActiveTrue(registerRequest.getOrganizationName())) {
                throw new DuplicateResourceException(
                    "An organization with the name '" + registerRequest.getOrganizationName() + "' already exists. If you work here, please ask your admin for an invite.", 
                    ErrorCode.BUSINESS_RULE_VIOLATION
                );
            }

            Organization organization = new Organization();
            organization.setName(registerRequest.getOrganizationName());
            organization.setCapabilities(registerRequest.getOrganizationCapabilities());

            organization.setContactNumber(registerRequest.getContactNumber());
            organization.setBillingAddress(registerRequest.getAddress());
            organization.setContactEmail(registerRequest.getEmail());

            savedOrganization = organizationRepository.save(organization);
        }

        User user = authMapper.toUserFromRegisterRequest(registerRequest);

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setOrganization(savedOrganization);

        User savedUser = userRepository.save(user);

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

        if (jwtService.validateToken(refreshTokenRequest.getRefreshToken(), userDetails)) {
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

        log.info("User requested logout. Frontend should clear tokens.");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
       
        long remainingTime = jwtService.getRemainingExpirationTime(token);

        if (remainingTime > 0) {
            try {
                redisTemplate.opsForValue().set(
                    "blacklist:" + token, 
                    "revoked", 
                    remainingTime, 
                    TimeUnit.MILLISECONDS
                );

                log.info("Token successfully blacklisted in Redis.");
            }
            catch (Exception e) {
                log.warn("Failed to blacklist token in Redis: {}. Token will expire naturally.", e.getMessage());
            }
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

        if (!jwtService.validateToken(request.getResetToken(), userDetails)) {
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

        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getUsername()));
        

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

        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getUsername()));

    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest verifyEmailRequest) {

        String verificationToken = verifyEmailRequest.getToken();

        UUID userId = jwtService.extractUserId(verificationToken);

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (!jwtService.validateToken(verificationToken, userDetails)) {
            throw new BusinessException("Invalid or expired verification token.", ErrorCode.UNAUTHORIZED);
        }

        String tokenPurpose = jwtService.extractPurpose(verificationToken);

        if (!"VERIFY_EMAIL".equals(tokenPurpose)) {
            throw new BusinessException("Invalid token.", ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new DuplicateResourceException("Email Already Verified.", ErrorCode.UNAUTHORIZED);
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getUsername()));
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
