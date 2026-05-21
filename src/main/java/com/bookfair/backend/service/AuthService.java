package com.bookfair.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.request.LoginRequest;
import com.bookfair.backend.dto.request.RegisterRequest;
import com.bookfair.backend.dto.response.AuthResponse;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.Role;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);


    public AuthResponse register(RegisterRequest registerRequest) {
        User user = new User();

        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
        user.setAddress(registerRequest.getAddress());
        user.setContactNumber(registerRequest.getContactNumber());
        user.setBusinessName(registerRequest.getBusinessName());

        try{
            user.setRole(Role.valueOf(registerRequest.getRole().toUpperCase()));
        }
        catch(Exception e){
            user.setRole(Role.VENDOR);
        }

        userRepository.save(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtService.generateToken(user));

        return authResponse;
        
    }   

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = 
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        
        User user = userRepository.findByUsername(loginRequest.getUsername());

        AuthResponse authResponse = new AuthResponse();

        if(!authentication.isAuthenticated()) {
            throw new RuntimeException("Invalid username or password");
        }

        authResponse.setToken(jwtService.generateToken(user));

        return authResponse;
    }
}
