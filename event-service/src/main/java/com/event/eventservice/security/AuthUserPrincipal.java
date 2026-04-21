package com.event.eventservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AuthUserPrincipal {

    private final Long userId;
    private final String email;
    private final List<String> roles;

    public AuthUserPrincipal(Long userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
