package com.bookfair.backend.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.model.RefreshToken;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagementService {

    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public RefreshToken createAndStoreRefreshToken(User user, String tokenString, long durationMillis,
            String ipAddress, String deviceInfo) {
        requireNonNull(user, "User cannot be null when storing refresh token session");
        requireNonNull(tokenString, "Token string cannot be null when storing refresh token session");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenString);
        refreshToken.setExpiryDate(Instant.now().plusMillis(durationMillis));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setDeviceInfo(deviceInfo);

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Created granular device session [{}] for user [{}] from IP [{}] on device [{}]",
                savedToken.getId(), user.getId(), ipAddress, deviceInfo);
        return savedToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String tokenString) {
        requireNonNull(tokenString, "Token string cannot be null during database session lookup");
        return refreshTokenRepository.findByToken(tokenString);
    }

    @Transactional
    public void revokeDeviceSession(UUID refreshTokenId) {
        requireNonNull(refreshTokenId, "RefreshToken ID cannot be null during single-device logout");
        refreshTokenRepository.deleteById(refreshTokenId);
        log.info("Revoked single device session [{}]", refreshTokenId);
    }

    @Transactional
    public void revokeDeviceSessionByToken(String tokenString) {
        requireNonNull(tokenString, "Token string cannot be null during single-device logout");
        refreshTokenRepository.deleteByToken(tokenString);
        log.info("Revoked single device session by token string");
    }

    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        requireNonNull(userId, "User ID cannot be null when revoking all user sessions");

        refreshTokenRepository.deleteByUserId(userId);
        log.info("Deleted all persistent database sessions for user [{}]", userId);

        tokenBlacklistService.createSecurityCheckpoint(userId);
    }

    public void storePasswordResetToken(String userId, String jti, long timeout, TimeUnit unit) {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(jti, "jti cannot be null");

        String key = "password_reset:" + userId;
        requireNonNull(redisTemplate.opsForValue()).set(key, jti, timeout, unit);
    }

    public void storeEmailVerificationToken(String userId, String jti, long timeout, TimeUnit unit) {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(jti, "jti cannot be null");

        String key = "email_verify:" + userId;
        requireNonNull(redisTemplate.opsForValue()).set(key, jti, timeout, unit);
    }

    public boolean consumePasswordResetToken(String userId, String jti) {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(jti, "jti cannot be null");

        String key = "password_reset:" + userId;
        String expectedJti = requireNonNull(redisTemplate.opsForValue()).get(key);

        if (jti.equals(expectedJti)) {
            redisTemplate.delete(requireNonNull(key));
            log.info("Successfully consumed password reset lock for user [{}]", userId);
            return true;
        }
        return false;
    }

    public boolean consumeEmailVerificationToken(String userId, String jti) {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(jti, "jti cannot be null");

        String key = "email_verify:" + userId;
        String expectedJti = requireNonNull(redisTemplate.opsForValue()).get(key);

        if (jti.equals(expectedJti)) {
            redisTemplate.delete(requireNonNull(key));
            log.info("Successfully consumed email verification lock for user [{}]", userId);
            return true;
        }
        return false;
    }
}
