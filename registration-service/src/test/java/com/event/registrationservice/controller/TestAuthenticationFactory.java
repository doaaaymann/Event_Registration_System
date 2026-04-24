package com.event.registrationservice.controller;

import com.event.registrationservice.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

final class TestAuthenticationFactory {

    private TestAuthenticationFactory() {
    }

    static Authentication authentication() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "participant@example.com", List.of("PARTICIPANT"));
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
