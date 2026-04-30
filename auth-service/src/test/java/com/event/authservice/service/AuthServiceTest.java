package com.event.authservice.service;

import com.event.authservice.dto.request.LoginRequest;
import com.event.authservice.dto.request.CreateManagedUserRequest;
import com.event.authservice.dto.request.RegisterRequest;
import com.event.authservice.dto.response.AuthResponse;
import com.event.authservice.dto.response.TokenValidationResponse;
import com.event.authservice.dto.response.UserResponse;
import com.event.authservice.entity.Role;
import com.event.authservice.entity.RoleName;
import com.event.authservice.entity.User;
import com.event.authservice.entity.UserStatus;
import com.event.authservice.exception.BadRequestException;
import com.event.authservice.repository.RoleRepository;
import com.event.authservice.repository.UserRepository;
import com.event.authservice.security.AuthUserPrincipal;
import com.event.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Role participantRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        participantRole = role(RoleName.PARTICIPANT);
        adminRole = role(RoleName.ADMIN);
    }

    @Test
    void registerCreatesParticipantUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Ali Hassan");
        request.setEmail("Ali@Example.com");
        request.setPassword("Secret123");
        request.setRole(RoleName.PARTICIPANT);

        when(userRepository.existsByEmailIgnoreCase("ali@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.PARTICIPANT)).thenReturn(Optional.of(participantRole));
        when(passwordEncoder.encode("Secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 2L);
            return user;
        });

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("ali@example.com");
        assertThat(savedUser.getFullName()).isEqualTo("Ali Hassan");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getRoles()).containsExactly(participantRole);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("ali@example.com");
        assertThat(response.getRoles()).containsExactly("PARTICIPANT");
    }

    @Test
    void registerRejectsNonParticipantPublicRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Organizer User");
        request.setEmail("organizer@example.com");
        request.setPassword("Secret123");
        request.setRole(RoleName.ORGANIZER);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Public registration only allows PARTICIPANT accounts");
    }

    @Test
    void loginReturnsJwtToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@event.local");
        request.setPassword("EventAdmin123!");

        User user = user(1L, "System Administrator", "admin@event.local", "hashed", adminRole);

        when(userRepository.findByEmailIgnoreCase("admin@event.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("EventAdmin123!", "hashed")).thenReturn(true);
        when(jwtService.generateToken(1L, "admin@event.local", List.of("ADMIN"))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void createManagedUserAllowsAdminToCreateOrganizer() {
        CreateManagedUserRequest request = new CreateManagedUserRequest();
        request.setFullName("Omar Organizer");
        request.setEmail("omar@example.com");
        request.setPassword("Secret123");
        request.setRole(RoleName.ORGANIZER);

        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
        Role organizerRole = role(RoleName.ORGANIZER);

        when(userRepository.existsByEmailIgnoreCase("omar@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ORGANIZER)).thenReturn(Optional.of(organizerRole));
        when(passwordEncoder.encode("Secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 3L);
            return user;
        });

        UserResponse response = authService.createManagedUser(principal, request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getEmail()).isEqualTo("omar@example.com");
        assertThat(response.getRoles()).containsExactly("ORGANIZER");
    }

    @Test
    void createManagedUserRejectsNonAdmin() {
        CreateManagedUserRequest request = new CreateManagedUserRequest();
        request.setFullName("Omar Organizer");
        request.setEmail("omar@example.com");
        request.setPassword("Secret123");
        request.setRole(RoleName.ORGANIZER);

        AuthUserPrincipal principal = new AuthUserPrincipal(7L, "ali@example.com", List.of("PARTICIPANT"));

        assertThatThrownBy(() -> authService.createManagedUser(principal, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access is denied");
    }

    @Test
    void loginRejectsBadPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@event.local");
        request.setPassword("wrong-password");

        User user = user(1L, "System Administrator", "admin@event.local", "hashed", adminRole);

        when(userRepository.findByEmailIgnoreCase("admin@event.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void getCurrentUserReturnsCurrentUser() {
        User user = user(7L, "Ali Hassan", "ali@example.com", "hashed", participantRole);
        AuthUserPrincipal principal = new AuthUserPrincipal(7L, "ali@example.com", List.of("PARTICIPANT"));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser(principal);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("ali@example.com");
        assertThat(response.getRoles()).containsExactly("PARTICIPANT");
    }

    @Test
    void validateReturnsPrincipalData() {
        AuthUserPrincipal principal = new AuthUserPrincipal(7L, "ali@example.com", List.of("PARTICIPANT"));

        TokenValidationResponse response = authService.validate(principal);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("ali@example.com");
        assertThat(response.getRoles()).containsExactly("PARTICIPANT");
    }

    @Test
    void ensureSelfOrAdminAllowsSelf() {
        AuthUserPrincipal principal = new AuthUserPrincipal(7L, "ali@example.com", List.of("PARTICIPANT"));

        authService.ensureSelfOrAdmin(principal, 7L);
    }

    @Test
    void ensureSelfOrAdminAllowsAdmin() {
        AuthUserPrincipal principal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));

        authService.ensureSelfOrAdmin(principal, 99L);
    }

    @Test
    void ensureSelfOrAdminRejectsOtherParticipant() {
        AuthUserPrincipal principal = new AuthUserPrincipal(7L, "ali@example.com", List.of("PARTICIPANT"));

        assertThatThrownBy(() -> authService.ensureSelfOrAdmin(principal, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access is denied");
    }

    private static Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    private static User user(Long id, String fullName, String email, String passwordHash, Role role) {
        User user = new User();
        setId(user, id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));
        return user;
    }

    private static void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to set user id in test", ex);
        }
    }
}
