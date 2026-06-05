package com.bookfair.backend.dto.auth.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.auth.response.AuthResponse;
import com.bookfair.backend.dto.auth.response.UserProfileResponse;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.model.User;

@Mapper(config = GlobalMapperConfig.class)
public interface AuthMapper {
    AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn);

    UserProfileResponse toUserProfileResponse(User user);
}
