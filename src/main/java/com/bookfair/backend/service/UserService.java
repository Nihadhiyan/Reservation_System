package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.reservation.mapper.ReservationMapper;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.user.mapper.UserMapper;
import com.bookfair.backend.dto.user.request.UpdateUserRequest;
import com.bookfair.backend.dto.user.response.UserResponse;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;

    public UserResponse getUserProfile(UUID userId) {
        User user =  userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + userId, 
                ErrorCode.USER_NOT_FOUND
            ));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest userUpdateRequest) {
        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + userId, 
                ErrorCode.USER_NOT_FOUND
            ));

        if (
            userUpdateRequest.getUsername() != null && 
            !userUpdateRequest.getUsername().equals(user.getUsername()) && 
            userRepository.existsByUsernameAndActiveTrue(userUpdateRequest.getUsername())
        ) {
            throw new DuplicateResourceException("Username is already taken.", ErrorCode.DUPLICATE_USERNAME);
        }

        if (userUpdateRequest.getEmail() != null && 
            !userUpdateRequest.getEmail().equals(user.getEmail()) &&
            userRepository.existsByEmailAndActiveTrue(userUpdateRequest.getEmail())
        ) {
            
            throw new DuplicateResourceException("That email is already in use by another account.", ErrorCode.DUPLICATE_EMAIL);
        }
        
        userMapper.updateUserFromRequest(userUpdateRequest, user);

        User updatedUser = userRepository.save(user);
        
        return userMapper.toUserResponse(updatedUser);
    }

    public void deleteUser(UUID userId) {
       User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + userId, 
                ErrorCode.USER_NOT_FOUND
            ));

        user.setActive(false);

        userRepository.save(user);
    }

    public List<ReservationResponse> getUserReservations(UUID userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with ID: " + userId, 
                ErrorCode.USER_NOT_FOUND
            ));
        

        return reservationRepository.findByUserOrderByCreatedAtDesc(user).stream().map(reservation -> {
            return reservationMapper.toReservationResponse(reservation);
        })
        .toList();
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByActiveTrue().stream()
            .map(user -> {
                return userMapper.toUserResponse(user);
            }).toList();
    }
    
}
