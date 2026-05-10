# OCL Constraints

This file shows the project constraints exactly the way they now appear in the codebase.

The project does not use a separate OCL runtime engine. Instead, each rule is represented in two code-level forms:

- an OCL invariant string inside a dedicated `ocl` class
- a Java validation method that enforces the same rule at runtime

Because of that, this document now uses the exact project snippets instead of only paraphrasing the rules.

## 1. Public Registration Is Limited To `PARTICIPANT`

### OCL invariant in code

File:
- [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)

```java
public static final String PUBLIC_REGISTRATION_PARTICIPANT_ONLY =
        "context RegisterRequest inv PublicRegistrationParticipantOnly: self.role = RoleName::PARTICIPANT";
```

### Runtime enforcement in code

File:
- [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)

```java
public static void requirePublicRegistrationParticipantOnly(RoleName role) {
    if (role != RoleName.PARTICIPANT) {
        throw new BadRequestException("Public registration only allows PARTICIPANT accounts");
    }
}
```

### Used by service

File:
- [AuthService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/service/AuthService.java:1)

```java
public UserResponse register(RegisterRequest request) {
    AuthOcl.requirePublicRegistrationParticipantOnly(request.getRole());
    return createUser(request.getFullName(), request.getEmail(), request.getPassword(), request.getRole());
}
```

## 2. Only `ADMIN` Can Create Managed Users

### OCL invariant in code

```java
public static final String ONLY_ADMIN_CREATES_MANAGED_USERS =
        "context AuthUserPrincipal inv OnlyAdminCreatesManagedUsers: self.roles->includes('ADMIN')";
```

### Runtime enforcement in code

File:
- [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)

```java
public static void requireAdminForManagedUserCreation(AuthUserPrincipal principal) {
    if (principal == null || principal.getUserId() == null
            || principal.getRoles() == null || !principal.getRoles().contains("ADMIN")) {
        throw new AccessDeniedException("Access is denied");
    }
}
```

### Used by service

```java
public UserResponse createManagedUser(AuthUserPrincipal principal, CreateManagedUserRequest request) {
    AuthOcl.requireAdminForManagedUserCreation(principal);
    if (request.getRole() == RoleName.ADMIN) {
        throw new BadRequestException("Use the seeded admin account for ADMIN access");
    }
    return createUser(request.getFullName(), request.getEmail(), request.getPassword(), request.getRole());
}
```

## 3. User Data Is Visible Only To The Same User Or `ADMIN`

### OCL invariant in code

File:
- [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)

```java
public static final String USER_VISIBLE_TO_SELF_OR_ADMIN =
        "context User inv UserVisibleToSelfOrAdmin: actingUser.roles->includes('ADMIN') or actingUser.id = self.id";
```

### Runtime enforcement in code

File:
- [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)

```java
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
```

### Used by service

File:
- [AuthService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/service/AuthService.java:1)

```java
public void ensureSelfOrAdmin(AuthUserPrincipal principal, Long userId) {
    AuthOcl.requireSelfOrAdmin(principal, userId);
}
```

## 4. Every Event Must Have At Least One Organizer

### OCL invariant in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static final String EVENT_MUST_HAVE_ORGANIZER =
        "context Event inv EventMustHaveOrganizer: self.organizerIds->size() >= 1";
```

### Runtime enforcement in code

File:
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:1)

```java
private List<Long> normalizeOrganizerIds(List<Long> organizerIds) {
    List<Long> normalized = organizerIds == null ? List.of() : organizerIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    if (normalized.isEmpty()) {
        throw new BadRequestException("At least one organizer is required");
    }
    return normalized;
}
```

## 5. Event End Time Must Be After Start Time

### OCL invariant in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static final String END_AFTER_START =
        "context Event inv EndAfterStart: self.endTime > self.startTime";
```

### Runtime enforcement in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static void requireEndAfterStart(LocalDateTime startTime, LocalDateTime endTime) {
    if (!endTime.isAfter(startTime)) {
        throw new BadRequestException("endTime must be after startTime");
    }
}
```

### Used by service

File:
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:1)

```java
public CreateEventResponse createEvent(AuthUserPrincipal principal, CreateEventRequest request) {
    EventOcl.requireOrganizerOrAdmin(principal);
    List<Long> organizerIds = normalizeOrganizerIds(request.getOrganizerIds());
    EventOcl.requirePrincipalOwnsOrganizerScope(principal, organizerIds);
    EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

    Event event = new Event();
    event.setTitle(request.getTitle().trim());
    event.setDescription(request.getDescription());
    event.setLocation(request.getLocation().trim());
    event.setStartTime(request.getStartTime());
    event.setEndTime(request.getEndTime());
    event.setMaxSeats(request.getMaxSeats());
    event.setOrganizerIds(organizerIds);
    event.setStatus(EventStatus.SCHEDULED);

    Event savedEvent = eventRepository.save(event);
    return new CreateEventResponse(
            savedEvent.getId(),
            savedEvent.getTitle(),
            savedEvent.getStatus(),
            savedEvent.getMaxSeats()
    );
}
```

## 6. Only Event Owners Or `ADMIN` Can Manage Event Changes

### OCL invariant in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static final String EVENT_MANAGED_BY_OWNER_OR_ADMIN =
        "context Event inv EventManagedByOwnerOrAdmin: actingUser.roles->includes('ADMIN') or self.organizerIds->includes(actingUser.id)";
```

### Runtime enforcement in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static void requireEventOwnerOrAdmin(AuthUserPrincipal principal, Event event) {
    requireOrganizerOrAdmin(principal);
    requirePrincipalOwnsOrganizerScope(principal, event.getOrganizerIds());
}
```

```java
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
```

### Used by service

File:
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:1)

```java
public EventResponse updateEvent(AuthUserPrincipal principal, Long eventId, UpdateEventRequest request) {
    Event event = getEventEntity(eventId);
    EventOcl.requireEventOwnerOrAdmin(principal, event);
    EventOcl.requireNotCancelled(event);
    EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

    event.setTitle(request.getTitle().trim());
    event.setDescription(request.getDescription());
    event.setLocation(request.getLocation().trim());
    event.setStartTime(request.getStartTime());
    event.setEndTime(request.getEndTime());
    event.setMaxSeats(request.getMaxSeats());

    return toEventResponse(eventRepository.save(event));
}
```

## 7. Cancelled Events Cannot Be Modified

### OCL invariant in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static final String CANCELLED_EVENT_NOT_EDITABLE =
        "context Event inv CancelledEventNotEditable: self.status = EventStatus::CANCELLED implies self.isEditable = false";
```

### Runtime enforcement in code

File:
- [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)

```java
public static void requireNotCancelled(Event event) {
    if (event.getStatus() == EventStatus.CANCELLED) {
        throw new BadRequestException("Cancelled events cannot be modified");
    }
}
```

### Used by service

File:
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:1)

```java
public EventResponse rescheduleEvent(AuthUserPrincipal principal, Long eventId, RescheduleEventRequest request) {
    Event event = getEventEntity(eventId);
    EventOcl.requireEventOwnerOrAdmin(principal, event);
    EventOcl.requireNotCancelled(event);
    EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

    event.setStartTime(request.getStartTime());
    event.setEndTime(request.getEndTime());
    event.setStatus(EventStatus.RESCHEDULED);

    Event savedEvent = eventRepository.save(event);
    sendEventRescheduledNotifications(savedEvent, principal);
    return toEventResponse(savedEvent);
}
```

## 8. No Duplicate Active Registration For The Same Participant And Event

### OCL-style rule represented by the implementation

File:
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:1)

### Runtime enforcement in code

```java
public RegistrationResponse createRegistration(AuthenticatedUser authenticatedUser, CreateRegistrationRequest request) {
    RegistrationOcl.requireAuthenticatedUser(authenticatedUser);
    RegistrationOcl.requireParticipantRole(authenticatedUser);
    if (request == null || request.getEventId() == null) {
        throw new BadRequestException("eventId is required");
    }
    return eventLockManager.executeWithLock(request.getEventId(),
            () -> {
                Long eventId = request.getEventId();
                if (registrationRepository.existsByEventIdAndParticipantIdAndStatus(
                        eventId, authenticatedUser.getUserId(), RegistrationStatus.REGISTERED)) {
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
                registration.setCancelledAt(null);

                Registration savedRegistration = registrationRepository.save(registration);
                sendCreatedNotification(savedRegistration, event);
                return toResponse(savedRegistration);
            });
}
```

## 9. Only `SCHEDULED` Or `RESCHEDULED` Events Accept Registration

### OCL invariant in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static final String REGISTRATION_ALLOWED_ONLY_FOR_OPEN_STATES =
        "context Event inv RegistrationAllowedOnlyForOpenStates: self.status = EventStatus::SCHEDULED or self.status = EventStatus::RESCHEDULED";
```

### Runtime enforcement in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static void requireRegistrableStatus(EventDetailsResponse event) {
    if (!isRegistrableStatus(event.getStatus())) {
        throw new BadRequestException("Only SCHEDULED or RESCHEDULED events can accept registrations");
    }
}
```

```java
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
```

### Used by service

File:
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:1)

```java
private void validateEventStatus(EventDetailsResponse event) {
    RegistrationOcl.requireRegistrableStatus(event);
}

private void validateAvailability(EventAvailabilityResponse availability) {
    RegistrationOcl.requireAvailableSeats(availability);
}
```

## 10. Only `PARTICIPANT` Users Can Register For Events

### OCL invariant in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static final String ONLY_PARTICIPANT_CAN_REGISTER =
        "context AuthenticatedUser inv OnlyParticipantCanRegister: self.roles->includes('PARTICIPANT')";
```

### Runtime enforcement in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static void requireParticipantRole(AuthenticatedUser authenticatedUser) {
    if (!authenticatedUser.hasRole("PARTICIPANT")) {
        throw new ForbiddenOperationException("Only PARTICIPANT users can register for events");
    }
}
```

### Used by service

File:
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:1)

```java
private void requireParticipantRole(AuthenticatedUser authenticatedUser) {
    RegistrationOcl.requireParticipantRole(authenticatedUser);
}
```

## 11. Registration Owner Only

### OCL invariant in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static final String REGISTRATION_OWNER_ONLY =
        "context Registration inv RegistrationOwnerOnly: actingUser.id = self.participantId";
```

### Runtime enforcement in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static void requireOwnership(Registration registration, AuthenticatedUser authenticatedUser) {
    if (!registration.getParticipantId().equals(authenticatedUser.getUserId())) {
        throw new ForbiddenOperationException("Only the registration owner can perform this action");
    }
}
```

### Used by service

File:
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:1)

```java
public RegistrationResponse getRegistration(Long registrationId, AuthenticatedUser authenticatedUser) {
    RegistrationOcl.requireAuthenticatedUser(authenticatedUser);
    Registration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
    RegistrationOcl.requireOwnership(registration, authenticatedUser);
    return toResponse(registration);
}
```

## 12. Event Participant Lists Are Restricted To Event Organizers Or `ADMIN`

### OCL invariant in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static final String PARTICIPANT_LIST_VISIBLE_TO_ORGANIZER_OR_ADMIN =
        "context Event inv ParticipantListVisibleToOrganizerOrAdmin: actingUser.roles->includes('ADMIN') or (actingUser.roles->includes('ORGANIZER') and self.organizerIds->includes(actingUser.id))";
```

### Runtime enforcement in code

File:
- [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)

```java
public static void requireParticipantTrackingAccess(AuthenticatedUser authenticatedUser, EventDetailsResponse event) {
    if (authenticatedUser.hasRole("ADMIN")) {
        return;
    }
    if (authenticatedUser.hasRole("ORGANIZER") && event.getOrganizerIds().contains(authenticatedUser.getUserId())) {
        return;
    }
    throw new ForbiddenOperationException("Only the event organizer or ADMIN can view event participants");
}
```

### Used by service

File:
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:1)

```java
public List<RegistrationResponse> getEventRegistrations(Long eventId, AuthenticatedUser authenticatedUser) {
    RegistrationOcl.requireAuthenticatedUser(authenticatedUser);
    EventDetailsResponse event = fetchEvent(eventId);
    RegistrationOcl.requireParticipantTrackingAccess(authenticatedUser, event);
    List<Registration> registrations = registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId);
    Map<Long, String> participantNames = getParticipantNames(registrations);
    return registrations
            .stream()
            .map(registration -> toResponse(registration, participantNames.get(registration.getParticipantId())))
            .toList();
}
```

## 13. Notifications Are Readable Only By Their Owner Or `ADMIN`

### OCL invariant in code

File:
- [NotificationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/ocl/NotificationOcl.java:1)

```java
public static final String NOTIFICATION_READABLE_BY_OWNER_OR_ADMIN =
        "context Notification inv NotificationReadableByOwnerOrAdmin: actingUser.roles->includes('ADMIN') or actingUser.id = self.userId";
```

### Runtime enforcement in code

File:
- [NotificationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/ocl/NotificationOcl.java:1)

```java
public static void requireReadPermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
    if (authenticatedUser.hasRole("ADMIN")) {
        return;
    }
    if (authenticatedUser.getUserId().equals(targetUserId)) {
        return;
    }
    throw new ForbiddenOperationException("Only the notification owner or ADMIN can perform this action");
}
```

### Used by service

File:
- [NotificationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/service/NotificationService.java:1)

```java
public List<NotificationResponse> getNotificationsByUserId(AuthenticatedUser authenticatedUser, Long userId) {
    NotificationOcl.requireAuthenticatedUser(authenticatedUser);
    NotificationOcl.requireReadPermission(authenticatedUser, userId);
    return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
}
```

## 14. Creating Notifications For Other Users Is An `ADMIN` Action

### OCL invariant in code

File:
- [NotificationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/ocl/NotificationOcl.java:1)

```java
public static final String NOTIFICATION_CREATE_PERMISSION =
        "context Notification inv NotificationCreatePermission: actingUser.roles->includes('ADMIN') or actingUser.id = self.userId";
```

### Runtime enforcement in code

File:
- [NotificationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/ocl/NotificationOcl.java:1)

```java
public static void requireCreatePermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
    if (authenticatedUser.hasRole("ADMIN")) {
        return;
    }
    if (authenticatedUser.getUserId().equals(targetUserId)) {
        return;
    }
    throw new ForbiddenOperationException("Only ADMIN can create notifications for other users");
}
```

### Used by service

File:
- [NotificationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/service/NotificationService.java:1)

```java
public NotificationResponse createNotification(AuthenticatedUser authenticatedUser, CreateNotificationRequest request) {
    NotificationOcl.requireAuthenticatedUser(authenticatedUser);
    validateCreateRequest(request);
    NotificationOcl.requireCreatePermission(authenticatedUser, request.getUserId());
    Notification notification = buildNotification(
            request.getUserId(),
            request.getType(),
            request.getTitle(),
            request.getMessage()
    );
    return toResponse(notificationRepository.save(notification));
}
```

## Summary

The OCL constraints are now represented directly in the source code itself.

You can point to:

- the dedicated OCL classes:
  - [AuthOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/ocl/AuthOcl.java:1)
  - [EventOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/ocl/EventOcl.java:1)
  - [RegistrationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/ocl/RegistrationOcl.java:1)
  - [NotificationOcl.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/ocl/NotificationOcl.java:1)

- the service-layer validation calls that enforce the same rules at runtime

So the project now has OCL-style constraints both in documentation and inside the codebase itself.
