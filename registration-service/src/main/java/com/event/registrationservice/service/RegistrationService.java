package com.event.registrationservice.service;

import com.event.registrationservice.client.EventServiceClient;
import com.event.registrationservice.client.NotificationServiceClient;
import com.event.registrationservice.dto.client.EventAvailabilityResponse;
import com.event.registrationservice.dto.client.EventDetailsResponse;
import com.event.registrationservice.dto.client.NotificationCommand;
import com.event.registrationservice.dto.request.CreateRegistrationRequest;
import com.event.registrationservice.dto.response.RegistrationResponse;
import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.exception.BadRequestException;
import com.event.registrationservice.exception.ConflictException;
import com.event.registrationservice.exception.DownstreamServiceException;
import com.event.registrationservice.exception.ForbiddenOperationException;
import com.event.registrationservice.exception.ResourceNotFoundException;
import com.event.registrationservice.repository.RegistrationRepository;
import com.event.registrationservice.security.AuthenticatedUser;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final String PARTICIPANT_ROLE = "PARTICIPANT";

    private final RegistrationRepository registrationRepository;
    private final EventServiceClient eventServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final EventLockManager eventLockManager;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventServiceClient eventServiceClient,
                               NotificationServiceClient notificationServiceClient,
                               EventLockManager eventLockManager) {
        this.registrationRepository = registrationRepository;
        this.eventServiceClient = eventServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.eventLockManager = eventLockManager;
    }

    @Transactional
    public RegistrationResponse createRegistration(AuthenticatedUser authenticatedUser, CreateRegistrationRequest request) {
        requireAuthenticatedUser(authenticatedUser);
        requireParticipantRole(authenticatedUser);
        if (request == null || request.getEventId() == null) {
            throw new BadRequestException("eventId is required");
        }
        return eventLockManager.executeWithLock(request.getEventId(),
                () -> {
                    Long eventId = request.getEventId();
                    if (registrationRepository.existsByEventIdAndParticipantId(eventId, authenticatedUser.getUserId())) {
                        throw new ConflictException("User is already registered for this event");
                    }

                    EventDetailsResponse event = fetchEvent(eventId);
                    validateEventStatus(event);

                    EventAvailabilityResponse availability = fetchAvailability(eventId);
                    validateAvailability(availability);

                    Registration registration = new Registration();
                    registration.setEventId(eventId);
                    registration.setParticipantId(authenticatedUser.getUserId());
                    registration.setStatus(RegistrationStatus.REGISTERED);
                    registration.setRegisteredAt(LocalDateTime.now());

                    Registration savedRegistration = registrationRepository.save(registration);
                    sendCreatedNotification(savedRegistration, event);
                    return toResponse(savedRegistration);
                });
    }

    @Transactional(readOnly = true)
    public RegistrationResponse getRegistration(Long registrationId, AuthenticatedUser authenticatedUser) {
        requireAuthenticatedUser(authenticatedUser);
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        enforceOwnership(registration, authenticatedUser);
        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getMyRegistrations(AuthenticatedUser authenticatedUser) {
        requireAuthenticatedUser(authenticatedUser);
        return registrationRepository.findAllByParticipantIdOrderByRegisteredAtDesc(authenticatedUser.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventRegistrations(Long eventId, AuthenticatedUser authenticatedUser) {
        requireAuthenticatedUser(authenticatedUser);
        EventDetailsResponse event = fetchEvent(eventId);
        enforceParticipantTrackingAccess(authenticatedUser, event);
        return registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RegistrationResponse cancelRegistration(Long registrationId, AuthenticatedUser authenticatedUser) {
        requireAuthenticatedUser(authenticatedUser);
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        enforceOwnership(registration, authenticatedUser);

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        Registration savedRegistration = registrationRepository.save(registration);

        EventDetailsResponse event = fetchEvent(savedRegistration.getEventId());
        sendCancelledNotification(savedRegistration, event);
        return toResponse(savedRegistration);
    }

    private void requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getUserId() == null) {
            throw new ForbiddenOperationException("Authenticated user context is required");
        }
    }

    private void requireParticipantRole(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.hasRole(PARTICIPANT_ROLE)) {
            throw new ForbiddenOperationException("Only PARTICIPANT users can register for events");
        }
    }

    private void enforceOwnership(Registration registration, AuthenticatedUser authenticatedUser) {
        if (!registration.getParticipantId().equals(authenticatedUser.getUserId())) {
            throw new ForbiddenOperationException("Only the registration owner can perform this action");
        }
    }

    private void enforceParticipantTrackingAccess(AuthenticatedUser authenticatedUser, EventDetailsResponse event) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.hasRole("ORGANIZER") && event.getOrganizerId() != null
                && event.getOrganizerId().equals(authenticatedUser.getUserId())) {
            return;
        }
        throw new ForbiddenOperationException("Only the event organizer or ADMIN can view event participants");
    }

    private void validateEventStatus(EventDetailsResponse event) {
        if (!"SCHEDULED".equalsIgnoreCase(event.getStatus())) {
            throw new BadRequestException("Only SCHEDULED events can accept registrations");
        }
    }

    private void validateAvailability(EventAvailabilityResponse availability) {
        if (!"SCHEDULED".equalsIgnoreCase(availability.getStatus())) {
            throw new BadRequestException("Only SCHEDULED events can accept registrations");
        }
        if (Boolean.FALSE.equals(availability.getRegistrationOpen())) {
            throw new BadRequestException("Registration is closed for this event");
        }
        if (availability.getAvailableSeats() == null || availability.getAvailableSeats() <= 0) {
            throw new BadRequestException("No seats available for this event");
        }
    }

    private EventDetailsResponse fetchEvent(Long eventId) {
        try {
            return eventServiceClient.getEvent(eventId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Event not found");
        } catch (FeignException ex) {
            throw new DownstreamServiceException("Event service is unavailable");
        }
    }

    private EventAvailabilityResponse fetchAvailability(Long eventId) {
        try {
            return eventServiceClient.getAvailability(eventId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Event availability not found");
        } catch (FeignException ex) {
            throw new DownstreamServiceException("Event service is unavailable");
        }
    }

    private void sendCreatedNotification(Registration registration, EventDetailsResponse event) {
        NotificationCommand command = new NotificationCommand(
                registration.getParticipantId(),
                "REGISTRATION_CONFIRMED",
                "Registration Confirmed",
                "You are registered for " + event.getTitle()
        );
        try {
            notificationServiceClient.sendRegistrationCreated(command);
        } catch (FeignException ex) {
            log.warn("Failed to send registration-created notification for registration {}: {}",
                    registration.getId(), ex.getMessage());
        }
    }

    private void sendCancelledNotification(Registration registration, EventDetailsResponse event) {
        NotificationCommand command = new NotificationCommand(
                registration.getParticipantId(),
                "REGISTRATION_CANCELLED",
                "Registration Cancelled",
                "Your registration was cancelled for " + event.getTitle()
        );
        try {
            notificationServiceClient.sendRegistrationCancelled(command);
        } catch (FeignException ex) {
            log.warn("Failed to send registration-cancelled notification for registration {}: {}",
                    registration.getId(), ex.getMessage());
        }
    }

    private RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getEventId(),
                registration.getParticipantId(),
                registration.getStatus().name(),
                registration.getRegisteredAt(),
                registration.getCancelledAt()
        );
    }
}
