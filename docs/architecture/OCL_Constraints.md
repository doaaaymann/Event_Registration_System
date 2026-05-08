# OCL Constraints

This document expresses key business rules for the Event Registration System using Object Constraint Language (OCL). These constraints mirror the rules already enforced in the implemented services.

## Context Assumptions

The expressions below assume the following conceptual model:

- `User`
- `Event`
- `Registration`
- `Notification`

Attributes and associations are written in a UML-style conceptual form for documentation purposes, even when the implementation is split across separate microservices.

## Event Constraints

### Every event must have at least one organizer

```ocl
context Event
inv AtLeastOneOrganizer:
  self.organizerIds->size() >= 1
```

### Event end time must be after start time

```ocl
context Event
inv ValidSchedule:
  self.endTime > self.startTime
```

### Event seat capacity must be positive

```ocl
context Event
inv PositiveSeatCapacity:
  self.maxSeats > 0
```

## Registration Constraints

### Only scheduled or rescheduled events can accept registrations

```ocl
context Registration
inv RegistrableEventStatus:
  self.status = RegistrationStatus::REGISTERED implies
  (self.event.status = EventStatus::SCHEDULED or
   self.event.status = EventStatus::RESCHEDULED)
```

### A participant cannot hold duplicate active registrations for the same event

```ocl
context User
inv NoDuplicateActiveRegistrations:
  self.registrations
    ->select(r | r.status = RegistrationStatus::REGISTERED)
    ->isUnique(r | r.event.id)
```

### Registered participants must use the PARTICIPANT role

```ocl
context Registration
inv OnlyParticipantCanRegister:
  self.status = RegistrationStatus::REGISTERED implies
  self.participant.roles->includes(RoleName::PARTICIPANT)
```

## Authorization Constraints

### Only admins can create managed organizer or participant accounts

```ocl
context User::createManagedUser(targetRole : RoleName)
pre AdminOnlyManagedAccountCreation:
  self.roles->includes(RoleName::ADMIN)
```

### Notifications are readable only by their owner or an admin

```ocl
context User::canReadNotification(n : Notification) : Boolean
body:
  self.roles->includes(RoleName::ADMIN) or n.user.id = self.id
```

## Notes

- These OCL constraints are documentation artifacts for the software engineering deliverable.
- Runtime enforcement in the current codebase is implemented through DTO validation, service-layer checks, database constraints, and authorization rules.
