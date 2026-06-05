package com.bookfair.backend.dto.auth.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;

    private String username;

    private String email;

    private String role;

    private String businessName;

    private String contactNumber;

    private String address;

    private Boolean active;

}
