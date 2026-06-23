package com.bookfair.backend.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.bookfair.backend.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwtSecret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60;

    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    private static final long PASSWORD_RESET_AND_VERIFICATION_TOKEN_EXPIRATION_TIME = 1000 * 60 * 15;

    private final StringRedisTemplate redisTemplate;

    public String generateAccessToken(User user) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("roles", "ROLE_" + user.getRole().name());

        String token = Jwts.builder()
                .claims()
                .add(claims)
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .and()
                .signWith(getKey())
                .compact();

        String sessionKey = "user_sessiions:" + user.getId().toString();

        redisTemplate.opsForSet().add(sessionKey, token);
        redisTemplate.expire(sessionKey, ACCESS_TOKEN_EXPIRATION_TIME, TimeUnit.MILLISECONDS);

        return token;
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(getKey())
                .compact();
    }

    public String generatePasswordResetToken(User user) {
        return Jwts.builder()
                .claim("purpose", "RESET_PASSWORD")
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(
                        new Date(System.currentTimeMillis() + PASSWORD_RESET_AND_VERIFICATION_TOKEN_EXPIRATION_TIME))
                .signWith(getKey())
                .compact();
    }

    public String generateVerificationToken(User user) {
        return Jwts.builder()
                .claim("purpose", "VERIFY_EMAIL")
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(
                        new Date(System.currentTimeMillis() + PASSWORD_RESET_AND_VERIFICATION_TOKEN_EXPIRATION_TIME))
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public String extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", String.class));
    }

    public String extractPurpose(String token) {
        return extractClaim(token, claims -> claims.get("purpose", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {

        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final UUID userId = extractUserId(token);

        if (userDetails instanceof CustomUserPrincipal principal) {
            return (userId.equals(principal.getId()) && !isTokenExpired(token));
        }

        return false;
    }

    public long getRemainingExpirationTime(String token) {

        Date expiration = extractExpiration(token);
        long remaining = expiration.getTime() - System.currentTimeMillis();

        return remaining > 0 ? remaining : 0;
    }

    private boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getAccessTokenExpirationTime() {
        return ACCESS_TOKEN_EXPIRATION_TIME;
    }
}
