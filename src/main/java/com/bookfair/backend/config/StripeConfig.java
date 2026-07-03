package com.bookfair.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    public void initStripe() {
        // This globally configures the Stripe library with your secret key
        Stripe.apiKey = stripeProperties.getApi().getKey();
    }
}
