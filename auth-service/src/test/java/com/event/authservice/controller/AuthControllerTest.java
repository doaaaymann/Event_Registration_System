package com.event.authservice.controller;

import com.event.authservice.dto.request.LoginRequest;
import com.event.authservice.dto.request.CreateManagedUserRequest;
import com.event.authservice.dto.request.RegisterRequest;
import com.event.authservice.dto.response.AuthResponse;
import com.event.authservice.dto.response.TokenValidationResponse;
import com.event.authservice.dto.response.UserResponse;
import com.event.authservice.security.AuthUserPrincipal;
import com.event.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest();
        UserResponse response = new UserResponse(2L, "Ali Hassan", "ali@example.com", "ACTIVE", List.of("PARTICIPANT"));

        when(authService.register(request)).thenReturn(response);

        var result = authController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).register(request);
    }

    @Test
    void loginReturnsOkResponse() {
        LoginRequest request = new LoginRequest();
        AuthResponse response = new AuthResponse("jwt-token", "Bearer", 1L, "admin@event.local", List.of("ADMIN"));

        when(authService.login(request)).thenReturn(response);

        var result = authController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).login(request);
    }

    @Test
    void createManagedUserReturnsCreatedResponse() {
        CreateManagedUserRequest request = new CreateManagedUserRequest();
        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
        UserResponse response = new UserResponse(3L, "Omar Organizer", "omar@example.com", "ACTIVE", List.of("ORGANIZER"));

        when(authService.createManagedUser(principal, request)).thenReturn(response);

        var result = authController.createManagedUser(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).createManagedUser(principal, request);
    }

    @Test
    void meReturnsCurrentUser() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "ali@example.com", List.of("PARTICIPANT"));
        UserResponse response = new UserResponse(2L, "Ali Hassan", "ali@example.com", "ACTIVE", List.of("PARTICIPANT"));

        when(authService.getCurrentUser(principal)).thenReturn(response);

        var result = authController.me(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).getCurrentUser(principal);
    }

    @Test
    void validateReturnsTokenInfo() {
        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
        TokenValidationResponse response = new TokenValidationResponse(true, 1L, "admin@event.local", List.of("ADMIN"));

        when(authService.validate(principal)).thenReturn(response);

        var result = authController.validate(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).validate(principal);
    }

    @Test
    void getUserChecksAuthorizationThenReturnsUser() {
        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
        UserResponse response = new UserResponse(2L, "Ali Hassan", "ali@example.com", "ACTIVE", List.of("PARTICIPANT"));

        when(authService.getUserById(2L)).thenReturn(response);

        var result = authController.getUser(principal, 2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authService).ensureSelfOrAdmin(principal, 2L);
        verify(authService).getUserById(2L);
    }

    @Test
    void getUserRolesChecksAuthorizationThenReturnsRoles() {
        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
        List<String> roles = List.of("PARTICIPANT");

        when(authService.getUserRoles(2L)).thenReturn(roles);

        var result = authController.getUserRoles(principal, 2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly("PARTICIPANT");
        verify(authService).ensureSelfOrAdmin(principal, 2L);
        verify(authService).getUserRoles(2L);
    }
}
