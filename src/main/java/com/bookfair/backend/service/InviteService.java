package com.bookfair.backend.service;

import com.bookfair.backend.dto.organization.request.InviteRequest;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.*;
import com.bookfair.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public void inviteUser(InviteRequest request) {
        Objects.requireNonNull(request, "Request must not be null");
        Objects.requireNonNull(request.getOrgId(), "Organization ID must not be null");
        Objects.requireNonNull(request.getEmail(), "Email must not be null");
        Objects.requireNonNull(request.getRole(), "Role must not be null");

        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        // Generate token
        String token = UUID.randomUUID().toString();

        OrganizationInvite invite = new OrganizationInvite();
        invite.setOrganizationId(org.getId());
        invite.setEmail(request.getEmail());
        invite.setAssignedRole(request.getRole());
        invite.setToken(token);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setUsed(false);

        inviteRepository.save(invite);

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

        OrganizationInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Invalid or expired token", ErrorCode.VALIDATION_ERROR));

        if (invite.getUsed()) {
            throw new BusinessException("Invite has already been used", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invite has expired", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new BusinessException("This invite was sent to a different email address",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        Organization org = organizationRepository.findById(invite.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        // Check if member already exists
        if (memberRepository.existsByUserIdAndOrganizationId(user.getId(), org.getId())) {
            throw new BusinessException("User is already a member of this organization",
                    ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        OrganizationMember member = new OrganizationMember();
        member.setUser(user);
        member.setOrganization(org);
        member.setRole(invite.getAssignedRole());

        memberRepository.save(member);

        invite.setUsed(true);
        inviteRepository.save(invite);
    }
}
