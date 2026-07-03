package com.bookfair.backend.dto.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bookfair.backend.dto.auth.request.RegisterRequest;
import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.dto.auth.response.UserProfileResponse;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.model.User;

@Mapper(config = GlobalMapperConfig.class)
public interface AuthMapper {
    AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn);

    UserProfileResponse toUserProfileResponse(User user);

    @Mapping(target = "password", ignore = true) // Ignore the raw password for security
    @Mapping(target = "active", constant = "true") // Automatically set active to true
    @Mapping(target = "emailVerified", constant = "false") // Automatically set verified to false
    User toUserFromRegisterRequest(RegisterRequest registerRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "token", source = "tokenString")
    @Mapping(target = "expiryDate", source = "expiryDate")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "deviceInfo", source = "deviceInfo")
    com.bookfair.backend.model.RefreshToken toRefreshToken(User user, String tokenString, java.time.Instant expiryDate, String ipAddress, String deviceInfo);
}

