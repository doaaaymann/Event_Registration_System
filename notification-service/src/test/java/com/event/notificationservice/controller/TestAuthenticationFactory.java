package com.event.notificationservice.controller;

import com.event.notificationservice.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

final class TestAuthenticationFactory {

    private TestAuthenticationFactory() {
    }

    static UsernamePasswordAuthenticationToken authentication() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "participant@example.com", List.of("PARTICIPANT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
