package com.bookfair.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.request.UserUpdateRequest;
import com.bookfair.backend.dto.response.UserResponse;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
        .map(user -> {
            UserResponse response = new UserResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setBusinessName(user.getBusinessName());
            response.setContactNumber(user.getContactNumber());
            response.setAddress(user.getAddress());
            return response;
        })
        .toList();
    }

    public User createUser(User user) {
        return userRepository.save(user);    
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setBusinessName(user.getBusinessName());
        response.setContactNumber(user.getContactNumber());
        response.setAddress(user.getAddress());
        return response;
    }

    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setUsername(userUpdateRequest.getUsername());
        user.setEmail(userUpdateRequest.getEmail());
        user.setBusinessName(userUpdateRequest.getBusinessName());
        user.setContactNumber(userUpdateRequest.getContactNumber());
        user.setAddress(userUpdateRequest.getAddress());

        userRepository.save(user);

        UserResponse userResponse = new UserResponse();
        
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setRole(user.getRole().name());
        userResponse.setBusinessName(user.getBusinessName());
        userResponse.setContactNumber(user.getContactNumber());
        userResponse.setAddress(user.getAddress());
        
        return userResponse;
    }

    public void deleteUser(Long id) {
       userRepository.deleteById(id);
    }
    
}
