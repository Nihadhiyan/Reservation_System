package com.bookfair.backend.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {
    private final StringRedisTemplate redisTemplate;

    public Boolean isAllowed(String key, int maxRequests, int timeWindowSeconds) {
        String redisKey = "rate_limit: " + key;

        try {
            Long currentRequests = redisTemplate.opsForValue().increment(redisKey);

            // If this is the first request in the window, set the expiration
            if (currentRequests != null && currentRequests == 1) {
                redisTemplate.expire(redisKey, timeWindowSeconds, TimeUnit.SECONDS);
            }

            if (currentRequests != null && currentRequests > maxRequests) {
                log.warn("Rate limit exceeded for key: {}", key);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Redis rate limiting failed, allowing request: {}", e.getMessage());
            return true;
        }
    }
}
