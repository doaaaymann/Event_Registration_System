package com.event.registrationservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.extractClaims(token);
            if (!jwtService.isTokenValid(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = claims.get("userId", Long.class);
            String subject = claims.getSubject();
            List<String> roles = extractRoles(claims);
            if (userId != null) {
                AuthenticatedUser principal = new AuthenticatedUser(userId, subject, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private List<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .toList();
        }
        Object roleClaim = claims.get("role");
        if (roleClaim instanceof String role) {
            return List.of(role.startsWith("ROLE_") ? role.substring(5) : role);
        }
        Object authoritiesClaim = claims.get("authorities");
        if (authoritiesClaim instanceof List<?> authorityList) {
            return authorityList.stream()
                    .map(item -> {
                        if (item instanceof String authority) {
                            return authority;
                        }
                        if (item instanceof Map<?, ?> map) {
                            Object authority = map.get("authority");
                            return authority instanceof String ? (String) authority : null;
                        }
                        return null;
                    })
                    .filter(authority -> authority != null && !authority.isBlank())
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                    .toList();
        }
        return Collections.emptyList();
    }
}
