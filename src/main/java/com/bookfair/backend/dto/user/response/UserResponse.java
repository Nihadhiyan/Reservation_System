package com.bookfair.backend.dto.user.response;

import java.io.Serializable;
import java.util.UUID;

import com.bookfair.backend.dto.common.SimpleOrganizationDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Implements Serializable for Redis caching compatibility
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String contactNumber;
    private String address;
    private SimpleOrganizationDto organization;
}
