# OCL Constraints

This document records the main business rules of the Event Registration System in an OCL-style form and shows where each rule is enforced in the code.

The project does not run a separate OCL engine at runtime. Instead, the constraints are documented formally here and enforced directly inside the backend services. That keeps the rules visible for analysis and grading while still making them practical in implementation.

## How To Read This File

Each rule includes:

- a short explanation of the business meaning
- an OCL-style constraint
- the backend file where the rule is enforced

The expressions are written in a readable, project-focused way so they stay close to the actual domain model.

## 1. Public Registration Is Limited To Participants

Only participant accounts can be created through public self-registration.

```ocl
context RegisterRequest
inv PublicRegistrationParticipantOnly:
    self.role = RoleName::PARTICIPANT
```

Implemented in:

- [AuthService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/service/AuthService.java:54)

The `register(...)` method rejects any public registration request whose role is not `PARTICIPANT`.

## 2. Only Admins Can Create Managed Accounts

Organizer and participant accounts created by management must be created by an admin.

```ocl
context AuthUserPrincipal
inv OnlyAdminCreatesManagedUsers:
    self.roles->includes('ADMIN')
```

Implemented in:

- [AuthService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/service/AuthService.java:77)
- [AuthService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/auth-service/src/main/java/com/event/authservice/service/AuthService.java:164)

The `createManagedUser(...)` method calls `ensureAdmin(...)` before creating the account.

## 3. Every Event Must Have At Least One Organizer

An event cannot be saved without organizer ownership.

```ocl
context Event
inv EventMustHaveOrganizer:
    self.organizerIds->size() >= 1
```

Implemented in:

- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:304)

The `normalizeOrganizerIds(...)` method removes invalid values and throws an error if no organizer remains.

## 4. Event End Time Must Be After Start Time

An event schedule is valid only when the end time is later than the start time.

```ocl
context Event
inv EndAfterStart:
    self.endTime > self.startTime
```

Implemented in:

- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:206)

The `validateSchedule(...)` method checks this rule during creation, update, and rescheduling.

## 5. Only Owners Or Admins Can Manage Event Changes

Updating, cancelling, or rescheduling an event is restricted to its assigned organizer scope or an admin.

```ocl
context Event
inv EventManagedByOwnerOrAdmin:
    actingUser.roles->includes('ADMIN') or
    self.organizerIds->includes(actingUser.id)
```

Implemented in:

- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:96)
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:113)
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:126)
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:165)
- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:188)

The service checks organizer ownership against the authenticated user, while admins are allowed to bypass the ownership check.

## 6. Cancelled Events Cannot Be Modified As Active Events

Once an event is cancelled, it cannot be edited like an active event.

```ocl
context Event
inv CancelledEventNotEditable:
    self.status = EventStatus::CANCELLED implies self.isEditable = false
```

Implemented in:

- [EventService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/event-service/src/main/java/com/event/eventservice/service/EventService.java:200)

The `ensureNotCancelled(...)` method is used before update, reschedule, and organizer reassignment operations.

## 7. A Participant Cannot Hold Duplicate Active Registrations For The Same Event

The same participant should not have more than one active registration for a single event.

```ocl
context Registration
inv NoDuplicateActiveRegistration:
    Registration.allInstances()
        ->select(r | r.eventId = self.eventId and
                     r.participantId = self.participantId and
                     r.status = RegistrationStatus::REGISTERED)
        ->size() <= 1
```

Implemented in:

- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:65)

Before saving a new registration, the service checks whether a `REGISTERED` record already exists for the same participant and event.

## 8. Only Scheduled Or Rescheduled Events Can Accept Registrations

Registrations are allowed only for events that are in a valid open state.

```ocl
context Event
inv RegistrationAllowedOnlyForOpenStates:
    self.status = EventStatus::SCHEDULED or
    self.status = EventStatus::RESCHEDULED
```

Implemented in:

- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:168)
- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:174)

The registration flow validates both the event details and the computed availability before allowing a participant to register.

## 9. Only Participants Can Register For Events

Event registration is reserved for users with the participant role.

```ocl
context AuthenticatedUser
inv OnlyParticipantCanRegister:
    self.roles->includes('PARTICIPANT')
```

Implemented in:

- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:146)

The service blocks registration attempts from non-participant accounts.

## 10. Registration Data Can Only Be Accessed By Its Owner

A registration record may be viewed or cancelled only by the participant who owns it.

```ocl
context Registration
inv RegistrationOwnerOnly:
    actingUser.id = self.participantId
```

Implemented in:

- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:152)

The `enforceOwnership(...)` method is used before reading or cancelling a registration.

## 11. Event Participant Lists Are Restricted To Event Organizers Or Admins

Only an assigned organizer for the event, or an admin, can inspect participant lists.

```ocl
context Event
inv ParticipantListVisibleToOrganizerOrAdmin:
    actingUser.roles->includes('ADMIN') or
    (actingUser.roles->includes('ORGANIZER') and
     self.organizerIds->includes(actingUser.id))
```

Implemented in:

- [RegistrationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java:158)

The participant query flow uses `enforceParticipantTrackingAccess(...)` to guard access.

## 12. Notifications Are Readable Only By Their Owner Or An Admin

A user should only see or mark notifications that belong to them, unless they are an admin.

```ocl
context Notification
inv NotificationReadableByOwnerOrAdmin:
    actingUser.roles->includes('ADMIN') or
    actingUser.id = self.userId
```

Implemented in:

- [NotificationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/service/NotificationService.java:127)

This rule is applied both when listing notifications for a user and when marking a notification as read.

## 13. Creating Notifications For Other Users Is An Admin Action

Regular users may create notifications only for themselves. Creating notifications for another user is an admin-only action.

```ocl
context Notification
inv NotificationCreatePermission:
    actingUser.roles->includes('ADMIN') or
    actingUser.id = self.userId
```

Implemented in:

- [NotificationService.java](/C:/Users/doaaa/Documents/GitHub/Event_Registration_System-/notification-service/src/main/java/com/event/notificationservice/service/NotificationService.java:117)

The `enforceCreatePermission(...)` method prevents cross-user notification creation unless the caller is an admin.

## Summary

The OCL work in this project exists in two complementary forms:

- formalized business constraints written here in OCL-style notation
- actual enforcement in the backend service layer

That structure fits the project well. The rules stay readable for documentation and grading, while the application still enforces them directly in production code.
