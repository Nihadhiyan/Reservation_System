package com.bookfair.backend.service;

import com.bookfair.backend.dto.organization.mapper.OrganizationMapper;
import com.bookfair.backend.dto.organization.request.InviteRequest;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.*;
import com.bookfair.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final OrganizationInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;
    private final OrganizationMapper organizationMapper;

    @Transactional
    public void inviteUser(InviteRequest request) {
        Objects.requireNonNull(request, "Request must not be null");
        Objects.requireNonNull(request.getOrgId(), "Organization ID must not be null");
        Objects.requireNonNull(request.getEmail(), "Email must not be null");
        Objects.requireNonNull(request.getRole(), "Role must not be null");

        Organization org = organizationRepository.findById(Objects.requireNonNull(request.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        // Generate token
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        OrganizationInvite invite = organizationMapper.toOrganizationInvite(org.getId(), request, token, expiresAt);

        inviteRepository.save(Objects.requireNonNull(invite));

        // Send email
        String acceptLink = "https://frontend-url/accept-invite?token=" + token;
        emailService.sendEmail(
                request.getEmail(),
                "You have been invited to join " + org.getName(),
                "email_verification_template",
                java.util.Map.of("userName", request.getEmail(), "verificationLink", acceptLink),
                null);
    }

    @Transactional
    public void acceptInvite(String token, UUID userId) {
        Objects.requireNonNull(token, "Token must not be null");
        Objects.requireNonNull(userId, "User ID must not be null");

        OrganizationInvite invite = inviteRepository.findByToken(Objects.requireNonNull(token))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Invalid or expired token", ErrorCode.VALIDATION_ERROR));

        if (invite.getUsed()) {
            throw new BusinessException("Invite has already been used", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Invite has expired", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new BusinessException("This invite was sent to a different email address",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        Organization org = organizationRepository.findById(Objects.requireNonNull(invite.getOrganizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        // Check if member already exists
        if (memberRepository.existsByUserIdAndOrganizationId(Objects.requireNonNull(user.getId()),
                Objects.requireNonNull(org.getId()))) {
            throw new BusinessException("User is already a member of this organization",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        OrganizationMember member = organizationMapper.toOrganizationMember(user, org, invite.getAssignedRole());

        memberRepository.save(Objects.requireNonNull(member));

        invite.setUsed(true);
        inviteRepository.save(Objects.requireNonNull(invite));
    }
}
