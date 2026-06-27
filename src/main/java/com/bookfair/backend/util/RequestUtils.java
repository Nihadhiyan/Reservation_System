package com.bookfair.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public final class RequestUtils {

    public static String getClientIpAddress(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest cannot be null when extracting client IP");

        // Inspecting X-Forwarded-For header injected by upstream load
        // balancers/proxies
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank() && !"unknown".equalsIgnoreCase(header)) {
            // X-Forwarded-For can contain a comma-separated list of proxies: "clientIp,
            // proxy1, proxy2"
            // The leftmost IP is always the original originating client address.
            String[] ips = header.split(",");
            String clientIp = ips[0].trim();
            return sanitizeIp(clientIp);
        }

        // Checking X-Real-IP fallback commonly set by NGINX ingress controllers
        header = request.getHeader("X-Real-IP");
        if (header != null && !header.isBlank() && !"unknown".equalsIgnoreCase(header)) {
            return sanitizeIp(header.trim());
        }

        // Fallback to raw socket remote address if no reverse proxy headers
        String remoteAddr = request.getRemoteAddr();
        return sanitizeIp(remoteAddr != null ? remoteAddr : "UNKNOWN");
    }

    public static String getDeviceInfo(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest cannot be null when extracting device info");

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN_DEVICE";
        }

        // Truncating length to 512 characters to prevent database column overflow
        return userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
    }

    // Ensuring extracted IP addresses fit within the database IPv6 column boundary
    // (45 characters).
    private static String sanitizeIp(String ip) {
        if (ip == null) {
            return "UNKNOWN";
        }
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
