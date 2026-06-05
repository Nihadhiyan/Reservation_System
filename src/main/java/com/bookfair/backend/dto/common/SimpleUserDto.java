package com.bookfair.backend.dto.common;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleUserDto {
    @NotNull(message = "Id is required")
    private UUID id;

    @NotBlank(message = "Username is required")
    private String username;
    
    @Email(message = "Email must be valid")
    private String email;
}
