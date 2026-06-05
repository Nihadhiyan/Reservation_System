package com.bookfair.backend.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemConfigRequest {
    
    @NotBlank(message = "Config key cannot be blank")
    private String configKey;

    @NotBlank(message = "Config value cannot be blank")
    private String configValue;
}
