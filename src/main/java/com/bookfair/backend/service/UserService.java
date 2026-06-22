package com.bookfair.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.reservation.mapper.ReservationMapper;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.user.mapper.UserMapper;
import com.bookfair.backend.dto.user.request.UpdateUserRequest;
import com.bookfair.backend.dto.user.request.UpdateUserRoleRequest;
import com.bookfair.backend.dto.user.response.UserResponse;
import com.bookfair.backend.event.UserUpdatedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ForbiddenException;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.DeletionAudit;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.Role;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId,
                        ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String username) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found",
                        ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String username, UpdateUserRequest userUpdateRequest) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username,
                        ErrorCode.USER_NOT_FOUND));


        if (userUpdateRequest.getUsername() != null &&
                !userUpdateRequest.getUsername().equals(user.getUsername()) &&
                userRepository.existsByUsernameAndActiveTrue(userUpdateRequest.getUsername())) {
            throw new DuplicateResourceException("Username is already taken.", ErrorCode.DUPLICATE_USERNAME);
        }

        if (userUpdateRequest.getEmail() != null &&
                !userUpdateRequest.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmailAndActiveTrue(userUpdateRequest.getEmail())) {

            throw new DuplicateResourceException("That email is already in use by another account.",
                    ErrorCode.DUPLICATE_EMAIL);
        }

        userMapper.updateUserFromRequest(userUpdateRequest, user);

        User updatedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getUsername()));

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUserAsAdmin(UUID userId) {

        User targetUser = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId,
                        ErrorCode.USER_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + getCurrentUserId(),
                        ErrorCode.USER_NOT_FOUND));

        UUID currentUserId = getCurrentUserId();

        if (currentUserId.equals(userId)) { 
            throw new BusinessException(
                "You cannot delete your own admin account.",
                ErrorCode.BUSINESS_RULE_VIOLATION
            );
        }

        if (targetUser.getRole() == Role.SUPER_ADMIN && userRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN) == 1) {
            throw new BusinessException("Cannot remove the last administrator", ErrorCode.FORBIDDEN);
        }

        if (requestingUser.getRole() == Role.ORG_ADMIN) {
            if (requestingUser.getOrganization() == null || targetUser.getOrganization() == null) {
                throw new ForbiddenException("Operation not permitted: Missing organization context.", ErrorCode.FORBIDDEN);
            }

            if (!requestingUser.getOrganization().getId().equals(targetUser.getOrganization().getId())) {
                throw new ForbiddenException("You cannot delete users outside your organization.", ErrorCode.FORBIDDEN);
            }

            if (targetUser.getRole() == Role.ORG_ADMIN) {
                long adminCount = userRepository.countByOrganizationIdAndRoleAndActiveTrue(
                    targetUser.getOrganization().getId(), Role.ORG_ADMIN
                );
                if (adminCount <= 1) {
                    throw new BusinessException("Cannot delete the last ORG_ADMIN.", ErrorCode.BUSINESS_RULE_VIOLATION);
                }
            }
        }

        targetUser.setActive(false);
        targetUser.setDeletionAudit(new DeletionAudit(LocalDateTime.now(), getCurrentUserId()));

        userRepository.save(targetUser);

        eventPublisher.publishEvent(new UserUpdatedEvent(targetUser.getId(), targetUser.getUsername()));

    }

    @Transactional
    public void deleteMyAccount(String username) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username,
                        ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException(
                    "Admin accounts cannot be deactivated",
                    ErrorCode.FORBIDDEN);
        }

        user.setActive(false);
        user.setDeletionAudit(new DeletionAudit(LocalDateTime.now(), getCurrentUserId()));

        userRepository.save(user);

        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getUsername()));

    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(UUID userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId,
                        ErrorCode.USER_NOT_FOUND));

        return reservationRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(reservationMapper::toReservationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String username) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username,
                        ErrorCode.USER_NOT_FOUND));

        return reservationRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(reservationMapper::toReservationResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable)
            .map(userMapper::toUserResponse);
    }

    @Transactional
    public void setRole(UUID id, UpdateUserRoleRequest updateUserRoleRequest) {
        User targetUser = userRepository.findByIdAndActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + id,
                ErrorCode.USER_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId()).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + getCurrentUserId(),
                ErrorCode.USER_NOT_FOUND));

        if (targetUser.getRole() == Role.SUPER_ADMIN
            && updateUserRoleRequest.getRole() != Role.SUPER_ADMIN
            && userRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN) == 1) {

            throw new BusinessException("Cannot change the role of last Super admin", ErrorCode.FORBIDDEN);
        }

        if (requestingUser.getRole() != Role.SUPER_ADMIN && requestingUser.getRole() == Role.ORG_ADMIN) {
            if (requestingUser.getOrganization() == null || targetUser.getOrganization() == null) {
                throw new ForbiddenException("Operation not permitted: Missing organization context.", ErrorCode.FORBIDDEN);
            }
            
            if (!requestingUser.getOrganization().getId().equals(targetUser.getOrganization().getId())) {
                throw new ForbiddenException("You cannot modify users outside your organization.", ErrorCode.FORBIDDEN);
            }
        }

        if (targetUser.getRole() == Role.ORG_ADMIN && updateUserRoleRequest.getRole() != Role.ORG_ADMIN) {
            long adminCount = userRepository.countByOrganizationIdAndRoleAndActiveTrue(
                targetUser.getOrganization().getId(), Role.ORG_ADMIN
            );
            if (adminCount <= 1) {
                throw new BusinessException("Cannot degrade the last ORG_ADMIN.", ErrorCode.BUSINESS_RULE_VIOLATION);
            }
        } 

        if (targetUser.getRole() == updateUserRoleRequest.getRole()) {
            throw new BusinessException(
                "User already has this role",
                ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        targetUser.setRole(updateUserRoleRequest.getRole());

        userRepository.save(targetUser);

        eventPublisher.publishEvent(new UserUpdatedEvent(targetUser.getId(), targetUser.getUsername()));

    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException(
            "Unable to resolve current user",
            ErrorCode.UNAUTHORIZED
        );
    }

}
