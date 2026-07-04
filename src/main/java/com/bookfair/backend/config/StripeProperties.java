package com.bookfair.backend.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;

@Data
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private final Api api = new Api();
    private final Webhook webhook = new Webhook();

    @Data
    public static class Api {

        @NotBlank(message = "Stripe API Key is missing!")
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private String key;
    }

    @Data
    public static class Webhook {

        @NotBlank(message = "Stripe Webhook Secret is missing!")
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private String secret;
    }
}
