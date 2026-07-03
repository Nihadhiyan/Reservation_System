package com.bookfair.backend.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.bookfair.backend.config.AppProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.bookfair.backend.model.User;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.repository.OrganizationMemberRepository;
import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60;

    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    private static final long PASSWORD_RESET_AND_VERIFICATION_TOKEN_EXPIRATION_TIME = 1000 * 60 * 15;

    private final OrganizationMemberRepository memberRepository;

    public String generateAccessToken(User user) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("roles", "ROLE_" + (user.getSystemRole() != null ? user.getSystemRole().name() : "CUSTOMER"));

        List<OrganizationMember> members = memberRepository.findByUserId(user.getId());
        Map<String, String> orgRoles = new HashMap<>();
        for (OrganizationMember member : members) {
            orgRoles.put(member.getOrganization().getId().toString(), member.getRole().name());
        }
        claims.put("org_roles", orgRoles);

        String token = Jwts.builder()
                .claims()
                .add(claims)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .and()
                .signWith(getKey())
                .compact();

        return token;
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
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
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwtSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, claims -> claims.getSubject()));
    }

    public Instant extractIssuedAt(String token) {
        return extractClaim(token, claims -> claims.getIssuedAt().toInstant());
    }

    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.getId());
    }

    public String extractSystemRole(String token) {
        return extractClaim(token, claims -> claims.get("roles", String.class));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> extractOrgRoles(String token) {
        return extractClaim(token, claims -> claims.get("org_roles", Map.class));
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

    public long getRemainingExpirationTime(String token) {

        Instant expiration = extractExpiration(token);
        long remaining = expiration.toEpochMilli() - System.currentTimeMillis();

        return remaining > 0 ? remaining : 0;
    }

    public boolean isTokenExpired(String token) {

        return extractExpiration(token).isBefore(Instant.now());
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration().toInstant());
    }

    public long getAccessTokenExpirationTime() {
        return ACCESS_TOKEN_EXPIRATION_TIME;
    }

    public long getRefreshTokenExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION_TIME;
    }

    public List<GrantedAuthority> extractAuthorities(String token) {

        List<GrantedAuthority> authorities = new ArrayList<>();

        String systemRole = extractSystemRole(token);

        if (systemRole != null && !systemRole.isBlank()) {
            authorities.add(new SimpleGrantedAuthority(systemRole));
        }

        Map<String, String> orgRoles = extractOrgRoles(token);

        if (orgRoles != null) {
            for (Map.Entry<String, String> entry : orgRoles.entrySet()) {
                String orgAuthority = "ORG_" + entry.getKey() + "_" + entry.getValue();
                authorities.add(new SimpleGrantedAuthority(orgAuthority));
            }
        }

        return authorities;
    }
}
