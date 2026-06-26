package com.bookfair.backend.config.filter;

import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bookfair.backend.security.CustomUserDetailsService;
import com.bookfair.backend.security.JwtService;
import com.bookfair.backend.service.SecurityManagementService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityManagementService securityManagementService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        UUID userId = null;
        String roles = null;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7);
        try {
            if (securityManagementService.isTokenBlacklisted(token)) {
                log.warn("Rejected request: Attempted access using a blacklisted token.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token has been revoked\"}");
                return;
            }
        } catch (Exception e) {
            log.error("SecurityManagementService blacklist lookup failed: {}", e.getMessage());
        }


        try {
            userId = jwtService.extractUserId(token);
            roles = jwtService.extractSystemRole(token);
        } catch (Exception e) {
            log.error("JWT Token extraction failed: {}", e.getMessage());
        }

        if (userId != null && roles != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (UsernameNotFoundException | DisabledException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized\"}");
                return;
            }

        }

        filterChain.doFilter(request, response);
    }

}
