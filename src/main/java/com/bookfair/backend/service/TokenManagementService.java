package com.bookfair.backend.service;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class TokenManagementService {

    private final StringRedisTemplate redisTemplate;

    public void storePasswordResetToken(String userId, String token, long timeout, TimeUnit unit) {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(token, "token cannot be null");
        String key = "password_reset:" + token;
        redisTemplate.opsForValue().set(key, userId, timeout, unit);
    }

    public void storeEmailVerificationToken(String userId, String token, long timeout, TimeUnit unit) {
        String key = "email_verification:" + token;
        redisTemplate.opsForValue().set(key, userId, timeout, unit);
    }
}
