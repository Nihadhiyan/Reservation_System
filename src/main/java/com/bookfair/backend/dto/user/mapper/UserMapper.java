
package com.bookfair.backend.dto.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.organization.mapper.OrganizationMapper;
import com.bookfair.backend.dto.user.request.UpdateUserRequest;
import com.bookfair.backend.dto.user.response.UserResponse;
import com.bookfair.backend.model.User;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {OrganizationMapper.class}
)
public interface UserMapper {

    UserResponse toUserResponse(User user);

    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
