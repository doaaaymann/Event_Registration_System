package com.event.eventservice.ocl;

import com.event.eventservice.entity.Event;
import com.event.eventservice.entity.EventStatus;
import com.event.eventservice.exception.BadRequestException;
import com.event.eventservice.security.AuthUserPrincipal;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;

public final class EventOcl {

    private EventOcl() {
    }

    public static final String EVENT_MUST_HAVE_ORGANIZER =
            "context Event inv EventMustHaveOrganizer: self.organizerIds->size() >= 1";

    public static final String END_AFTER_START =
            "context Event inv EndAfterStart: self.endTime > self.startTime";

    public static final String EVENT_MANAGED_BY_OWNER_OR_ADMIN =
            "context Event inv EventManagedByOwnerOrAdmin: actingUser.roles->includes('ADMIN') or self.organizerIds->includes(actingUser.id)";

    public static final String CANCELLED_EVENT_NOT_EDITABLE =
            "context Event inv CancelledEventNotEditable: self.status = EventStatus::CANCELLED implies self.isEditable = false";

    public static void requireOrganizerOrAdmin(AuthUserPrincipal principal) {
        if (principal == null || principal.getRoles() == null || principal.getRoles().stream().noneMatch(role ->
                "ADMIN".equals(role) || "ORGANIZER".equals(role))) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public static void requireAdmin(AuthUserPrincipal principal) {
        if (principal == null || principal.getUserId() == null
                || principal.getRoles() == null || !principal.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public static void requirePrincipalOwnsOrganizerScope(AuthUserPrincipal principal, List<Long> organizerIds) {
        if (principal == null || principal.getUserId() == null || principal.getRoles() == null) {
            throw new AccessDeniedException("Access is denied");
        }
        if (principal.getRoles().contains("ADMIN")) {
            return;
        }
        if (organizerIds == null || organizerIds.isEmpty() || organizerIds.stream().anyMatch(id -> !principal.getUserId().equals(id))) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public static void requireEventOwnerOrAdmin(AuthUserPrincipal principal, Event event) {
        requireOrganizerOrAdmin(principal);
        requirePrincipalOwnsOrganizerScope(principal, event.getOrganizerIds());
    }

    public static void requireNotCancelled(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cancelled events cannot be modified");
        }
    }

    public static void requireEndAfterStart(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }
}
