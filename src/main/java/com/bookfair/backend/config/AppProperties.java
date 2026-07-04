package com.bookfair.backend.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "JWT Secret is missing!")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String jwtSecret;
    private final Cors cors = new Cors();
    private final Security security = new Security();

    @Data
    public static class Cors {
        @NotEmpty(message = "CORS allowed origins must be configured!")
        private List<String> allowedOrigins;
    }

    @Data
    public static class Security {
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private String piiSecret;
    }
}
