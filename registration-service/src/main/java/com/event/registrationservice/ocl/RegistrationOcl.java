package com.event.registrationservice.ocl;

import com.event.registrationservice.dto.client.EventAvailabilityResponse;
import com.event.registrationservice.dto.client.EventDetailsResponse;
import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.exception.BadRequestException;
import com.event.registrationservice.exception.ForbiddenOperationException;
import com.event.registrationservice.security.AuthenticatedUser;

public final class RegistrationOcl {

    private RegistrationOcl() {
    }

    public static final String ONLY_PARTICIPANT_CAN_REGISTER =
            "context AuthenticatedUser inv OnlyParticipantCanRegister: self.roles->includes('PARTICIPANT')";

    public static final String REGISTRATION_OWNER_ONLY =
            "context Registration inv RegistrationOwnerOnly: actingUser.id = self.participantId";

    public static final String REGISTRATION_ALLOWED_ONLY_FOR_OPEN_STATES =
            "context Event inv RegistrationAllowedOnlyForOpenStates: self.status = EventStatus::SCHEDULED or self.status = EventStatus::RESCHEDULED";

    public static final String PARTICIPANT_LIST_VISIBLE_TO_ORGANIZER_OR_ADMIN =
            "context Event inv ParticipantListVisibleToOrganizerOrAdmin: actingUser.roles->includes('ADMIN') or (actingUser.roles->includes('ORGANIZER') and self.organizerIds->includes(actingUser.id))";

    public static void requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getUserId() == null) {
            throw new ForbiddenOperationException("Authenticated user context is required");
        }
    }

    public static void requireParticipantRole(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.hasRole("PARTICIPANT")) {
            throw new ForbiddenOperationException("Only PARTICIPANT users can register for events");
        }
    }

    public static void requireOwnership(Registration registration, AuthenticatedUser authenticatedUser) {
        if (!registration.getParticipantId().equals(authenticatedUser.getUserId())) {
            throw new ForbiddenOperationException("Only the registration owner can perform this action");
        }
    }

    public static void requireParticipantTrackingAccess(AuthenticatedUser authenticatedUser, EventDetailsResponse event) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.hasRole("ORGANIZER") && event.getOrganizerIds().contains(authenticatedUser.getUserId())) {
            return;
        }
        throw new ForbiddenOperationException("Only the event organizer or ADMIN can view event participants");
    }

    public static void requireRegistrableStatus(EventDetailsResponse event) {
        if (!isRegistrableStatus(event.getStatus())) {
            throw new BadRequestException("Only SCHEDULED or RESCHEDULED events can accept registrations");
        }
    }

    public static void requireAvailableSeats(EventAvailabilityResponse availability) {
        if (!isRegistrableStatus(availability.getStatus())) {
            throw new BadRequestException("Only SCHEDULED or RESCHEDULED events can accept registrations");
        }
        if (Boolean.FALSE.equals(availability.getRegistrationOpen())) {
            throw new BadRequestException("Registration is closed for this event");
        }
        if (availability.getAvailableSeats() == null || availability.getAvailableSeats() <= 0) {
            throw new BadRequestException("No seats available for this event");
        }
    }

    private static boolean isRegistrableStatus(String status) {
        return "SCHEDULED".equalsIgnoreCase(status) || "RESCHEDULED".equalsIgnoreCase(status);
    }
}
