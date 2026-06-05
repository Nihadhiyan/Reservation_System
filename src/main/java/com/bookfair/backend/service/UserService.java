package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.reservation.mapper.ReservationMapper;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.user.mapper.UserMapper;
import com.bookfair.backend.dto.user.request.UpdateUserRequest;
import com.bookfair.backend.dto.user.response.UserResponse;
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
        User user =  userRepository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(UUID userId, UpdateUserRequest userUpdateRequest) {
        User user = userRepository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        userMapper.updateUserFromRequest(userUpdateRequest, user);

        User updatedUser = userRepository.save(user);
        
        return userMapper.toUserResponse(updatedUser);
    }

    public void deleteUser(UUID userId) {
       User user = userRepository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);

        userRepository.save(user);
    }

    public List<ReservationResponse> getUserReservations(UUID userId) {
        User user = userRepository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        

        return reservationRepository.findByUserOrderByCreatedAtDesc(user).stream().map(reservation -> {
            return reservationMapper.toReservationResponse(reservation);
        })
        .toList();
    }
    
}
