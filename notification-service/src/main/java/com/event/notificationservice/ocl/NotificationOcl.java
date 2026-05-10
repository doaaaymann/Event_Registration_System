package com.event.notificationservice.ocl;

import com.event.notificationservice.exception.ForbiddenOperationException;
import com.event.notificationservice.security.AuthenticatedUser;

public final class NotificationOcl {

    private NotificationOcl() {
    }

    public static final String NOTIFICATION_READABLE_BY_OWNER_OR_ADMIN =
            "context Notification inv NotificationReadableByOwnerOrAdmin: actingUser.roles->includes('ADMIN') or actingUser.id = self.userId";

    public static final String NOTIFICATION_CREATE_PERMISSION =
            "context Notification inv NotificationCreatePermission: actingUser.roles->includes('ADMIN') or actingUser.id = self.userId";

    public static void requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getUserId() == null) {
            throw new ForbiddenOperationException("Authenticated user context is required");
        }
    }

    public static void requireCreatePermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.getUserId().equals(targetUserId)) {
            return;
        }
        throw new ForbiddenOperationException("Only ADMIN can create notifications for other users");
    }

    public static void requireReadPermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.getUserId().equals(targetUserId)) {
            return;
        }
        throw new ForbiddenOperationException("Only the notification owner or ADMIN can perform this action");
    }
}
