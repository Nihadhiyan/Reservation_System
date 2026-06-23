package com.bookfair.backend.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.bookfair.backend.service.RateLimitingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    // Define your global limits here
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long TIME_WINDOW_SECONDS = 60;

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract client IP
        String clientKey = determineClientIdentifier(request);

        boolean allowed = rateLimitingService.isAllowed(clientKey, MAX_REQUESTS_PER_MINUTE, TIME_WINDOW_SECONDS);

        if (!allowed) {

            response.setStatus(HttpStatus.SC_TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter("{\"error\": \"Too many requests. Please try again later\"}");
            return;

        }

        filterChain.doFilter(request, response);

    }

    private String determineClientIdentifier(HttpServletRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            // Rate limit by authenticated username/ID
            return "user:" + authentication.getName();
        }

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        return "ip:" + ipAddress;
    }
}
