package com.event.authservice.controller;

import com.event.authservice.dto.request.LoginRequest;
import com.event.authservice.dto.request.CreateManagedUserRequest;
import com.event.authservice.dto.request.RegisterRequest;
import com.event.authservice.dto.request.UpdateProfileRequest;
import com.event.authservice.dto.response.AuthResponse;
import com.event.authservice.dto.response.TokenValidationResponse;
import com.event.authservice.dto.response.UserResponse;
import com.event.authservice.security.AuthUserPrincipal;
import com.event.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<UserResponse> createManagedUser(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody CreateManagedUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createManagedUser(principal, request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                 @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateCurrentUser(principal, request));
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ResponseEntity.ok(authService.validate(principal));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                @PathVariable("userId") Long userId) {
        authService.ensureSelfOrAdmin(principal, userId);
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<String>> getUserRoles(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                     @PathVariable("userId") Long userId) {
        authService.ensureSelfOrAdmin(principal, userId);
        return ResponseEntity.ok(authService.getUserRoles(userId));
    }
}
