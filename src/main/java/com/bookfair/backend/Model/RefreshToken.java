package com.bookfair.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Enterprise Stateful Session Entity (RefreshToken).
 * <p>
 * This entity stores granular user device sessions. By tracking each issued
 * refresh token
 * alongside device metadata (IP address and User-Agent), security analysts can
 * detect
 * anomalous login patterns or compromised devices, and users/admins can perform
 * targeted
 * single-device logouts (session revocations).
 * </p>
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_string", columnList = "token", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The cryptographically secure refresh token string (or JWT).
     * Indexed and unique to ensure fast O(1) lookups during the /refresh-token
     * flow.
     */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /**
     * The authenticated user owning this device session.
     * Lazy fetched to avoid unnecessary join overhead during routine token lookups.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The exact UTC instant when this refresh token expires.
     * Evaluated during token refresh attempts to reject expired sessions.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * The client IP address from which the session was initiated.
     * Supports IPv6 addresses up to 45 characters.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * The client device information extracted from the User-Agent header.
     * Helps users identify recognized sessions (e.g., "Chrome on macOS").
     */
    @Column(name = "device_info", length = 512)
    private String deviceInfo;

    /**
     * Validates whether this token session has expired relative to the current UTC
     * instant.
     *
     * @return true if the token is expired, false otherwise.
     */
    public boolean isExpired() {
        Objects.requireNonNull(this.expiryDate, "Expiry date cannot be null during expiration check");
        return Instant.now().isAfter(this.expiryDate);
    }
}
