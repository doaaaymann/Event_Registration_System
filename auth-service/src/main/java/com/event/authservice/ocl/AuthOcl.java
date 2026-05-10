package com.event.authservice.ocl;

import com.event.authservice.entity.RoleName;
import com.event.authservice.exception.BadRequestException;
import com.event.authservice.security.AuthUserPrincipal;
import org.springframework.security.access.AccessDeniedException;

public final class AuthOcl {

    private AuthOcl() {
    }

    public static final String PUBLIC_REGISTRATION_PARTICIPANT_ONLY =
            "context RegisterRequest inv PublicRegistrationParticipantOnly: self.role = RoleName::PARTICIPANT";

    public static final String ONLY_ADMIN_CREATES_MANAGED_USERS =
            "context AuthUserPrincipal inv OnlyAdminCreatesManagedUsers: self.roles->includes('ADMIN')";

    public static final String USER_VISIBLE_TO_SELF_OR_ADMIN =
            "context User inv UserVisibleToSelfOrAdmin: actingUser.roles->includes('ADMIN') or actingUser.id = self.id";

    public static void requirePublicRegistrationParticipantOnly(RoleName role) {
        if (role != RoleName.PARTICIPANT) {
            throw new BadRequestException("Public registration only allows PARTICIPANT accounts");
        }
    }

    public static void requireAdminForManagedUserCreation(AuthUserPrincipal principal) {
        if (principal == null || principal.getUserId() == null
                || principal.getRoles() == null || !principal.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public static void requireSelfOrAdmin(AuthUserPrincipal principal, Long userId) {
        if (principal == null || principal.getUserId() == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        boolean isAdmin = principal.getRoles() != null && principal.getRoles().contains("ADMIN");
        boolean isSelf = principal.getUserId().equals(userId);
        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
