package com.event.registrationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @Test
    void filterExtractsUserIdAndRolesFromJwt() throws Exception {
        String secret = Base64.getEncoder().encodeToString("ThisIsASecureAndSufficientlyLongBase64SecretKeyForRegistrationService"
                .getBytes(StandardCharsets.UTF_8));
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        JwtService jwtService = new JwtService(properties);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        SecretKey signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        String token = Jwts.builder()
                .subject("participant@example.com")
                .claim("userId", 55L)
                .claim("roles", List.of("PARTICIPANT"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(55L);
        assertThat(principal.getRoles()).containsExactly("PARTICIPANT");
        SecurityContextHolder.clearContext();
    }
}
