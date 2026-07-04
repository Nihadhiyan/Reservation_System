package com.bookfair.backend.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current Password is required")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String oldPassword;

    @NotBlank(message = "New Password is required")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String newPassword;
}
