package com.bookfair.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.Role;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.CustomUserDetailsService;
import com.bookfair.backend.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final CustomUserDetailsService userDetailsService;

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

        userDetailsService.evictUserDetails(updatedUser);

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUserAsAdmin(UUID userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId,
                        ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN && userRepository.countByRoleAndActiveTrue(Role.ADMIN) == 1) {
            throw new BusinessException("Cannot remove the last administrator", ErrorCode.FORBIDDEN);
        }

        user.setActive(false);
        user.getDeletionAudit().setDeletedBy(getCurrentUserId());
        user.getDeletionAudit().setDeletedAt(LocalDateTime.now());

        userRepository.save(user);

        userDetailsService.evictUserDetails(user);

    }

    @Transactional
    public void deleteMyAccount(String username) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username,
                        ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(
                    "Admin accounts cannot be deactivated",
                    ErrorCode.FORBIDDEN);
        }

        user.setActive(false);
        user.getDeletionAudit().setDeletedBy(getCurrentUserId());
        user.getDeletionAudit().setDeletedAt(LocalDateTime.now());

        userRepository.save(user);

        userDetailsService.evictUserDetails(user);

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
        User user = userRepository.findByIdAndActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + id,
                ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN
            && updateUserRoleRequest.getRole() != Role.ADMIN
            && userRepository.countByRoleAndActiveTrue(Role.ADMIN) == 1) {

            throw new BusinessException("Cannot change the role of last administrator", ErrorCode.FORBIDDEN);
        }

        if (user.getRole() == updateUserRoleRequest.getRole()) {
             throw new BusinessException(
                "User already has this role",
                ErrorCode.BUSINESS_RULE_VIOLATION);
        
        }

        user.setRole(updateUserRoleRequest.getRole());

        userRepository.save(user);

        userDetailsService.evictUserDetails(user);

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
