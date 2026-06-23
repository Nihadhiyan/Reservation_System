package com.bookfair.backend.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bookfair.backend.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityManagementService {

    private final StringRedisTemplate redisTemplate;
    private final CustomUserDetailsService userDetailsService;

    private static final long DEFAULT_BLACKLIST_TTL_SECONDS = 60 * 60 * 24 * 7;

    public void handleRevocation(UUID userId) {
        // Blacklist tokens (Pipelined)
        revokeTokens(userId);

        // Clear cache
        userDetailsService.evictUserDetails(userId, null);
    }

    private void revokeTokens(UUID userId) {
        String sessionKey = "user_sessiions:" + userId; // Match typo in JwtService "user_sessiions"

        // Getting all session tokens for this user
        Set<String> tokens = redisTemplate.opsForSet().members(sessionKey);

        if (tokens != null && !tokens.isEmpty()) {
            try {
                // Executing pipelined operations
                redisTemplate.executePipelined(
                        (RedisCallback<Object>) connection -> {
                            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                            // Blacklisting the tokens
                            for (String token : tokens) {
                                stringRedisConn.setEx("blacklist:" + token, DEFAULT_BLACKLIST_TTL_SECONDS, "revoked");
                            }

                            // Removing the session set itself
                            stringRedisConn.del(sessionKey);

                            return null;
                        });

                log.info("Revoked {} session(s) for user {}", tokens.size(), userId);
            } catch (Exception e) {
                log.warn("Failed to revoke sessions for user {}: {}", userId, e.getMessage());
            }
        }
    }

    // ─── Called on normal single-device logout ────────────────────────────────

    public void blacklistToken(String token, long remainingTimeMs) {
        if (remainingTimeMs <= 0) {
            return;
        }

        long ttlSeconds = remainingTimeMs / 1000;

        try {
            // blacklist: prefix matches JwtAuthenticationFilter check
            redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "revoked",
                    ttlSeconds,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public void removeSession(UUID userId, String token) {
        redisTemplate.opsForSet().remove("user_sessions:" + userId, token);
    }

    // ─── Optional check utility ───────────────────────────────────────────────

    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
        } catch (Exception e) {
            log.warn("Redis blacklist check failed, defaulting to not blacklisted: {}", e.getMessage());
            return false; // fail-open, same as your JwtAuthenticationFilter
        }
    }
}
