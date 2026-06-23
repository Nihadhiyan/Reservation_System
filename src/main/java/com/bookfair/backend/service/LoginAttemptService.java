package com.bookfair.backend.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPTS_TTL_MINUTES = 15;
    private static final long LOCK_TTL_MINUTES = 30;

    public void recordFailedAttempt(String username) {
        String attemptsKey = "login_attempts:" + username;
        String lockKey = "lock:" + username;

        // If already locked, do nothing (or reset lock, but standard is do nothing)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            return;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, ATTEMPTS_TTL_MINUTES, TimeUnit.MINUTES);
        }

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(attemptsKey); // Clear attempts once locked
        }
    }

    public boolean isLocked(String username) {
        String lockKey = "lock:" + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    public void resetAttempts(String username) {
        String attemptsKey = "login_attempts:" + username;
        redisTemplate.delete(attemptsKey);
    }
}
