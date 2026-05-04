package com.event.authservice.dto.response;

import java.util.List;

public class UserResponse {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String interests;
    private final String status;
    private final List<String> roles;

    public UserResponse(Long id, String fullName, String email, String interests, String status, List<String> roles) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.interests = interests;
        this.status = status;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getInterests() {
        return interests;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getRoles() {
        return roles;
    }
}
