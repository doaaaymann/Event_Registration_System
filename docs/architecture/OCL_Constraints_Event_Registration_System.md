
## 1. Class: User

```ocl
-- INV-USR-01: full name must not be empty
context User
inv fullNameNotEmpty:
  self.fullName <> null and self.fullName.size() > 0

-- INV-USR-02: email must not be empty
context User
inv emailNotEmpty:
  self.email <> null and self.email.size() > 0

-- INV-USR-03: email must be unique across all users
context User
inv emailUnique:
  User.allInstances()->isUnique(u | u.email)

-- INV-USR-04: password hash must not be empty
context User
inv passwordHashNotEmpty:
  self.passwordHash <> null and self.passwordHash.size() > 0

-- INV-USR-05: every user must have at least one role
context User
inv hasRole:
  self.role <> null

-- PRE-USR-01: login requires non-empty credentials
context User::login(email: String, password: String)
pre credentialsProvided:
  email <> null and email.size() > 0 and
  password <> null and password.size() > 0

-- POST-USR-01: successful login returns a non-empty token
context User::login(email: String, password: String)
post tokenReturned:
  result <> null and result.size() > 0
```

---

## 2. Class: Role

```ocl
-- INV-ROL-01: role name must be one of the defined role literals
context Role
inv validRoleName:
  self.name = RoleName::ADMIN or
  self.name = RoleName::ORGANIZER or
  self.name = RoleName::PARTICIPANT
```

---

## 3. Class: Event

```ocl
-- INV-EVT-01: event title must not be empty
context Event
inv titleNotEmpty:
  self.title <> null and self.title.size() > 0

-- INV-EVT-02: event location must not be empty
context Event
inv locationNotEmpty:
  self.location <> null and self.location.size() > 0

-- INV-EVT-03: event end time must be after start time
context Event
inv validEventTime:
  self.endTime > self.startTime

-- INV-EVT-04: event capacity must be positive
context Event
inv maxSeatsPositive:
  self.maxSeats > 0

-- INV-EVT-05: every event must have at least one organizer
context Event
inv hasOrganizer:
  OrganizerAssignment.allInstances()->exists(a |
    a.eventId = self.id
  )

-- INV-EVT-06: all assigned organizers must have the ORGANIZER role
context Event
inv organizersHaveOrganizerRole:
  OrganizerAssignment.allInstances()->select(a |
    a.eventId = self.id
  )->forAll(a |
    User.allInstances()->exists(u |
      u.id = a.organizerId and
      u.role.name = RoleName::ORGANIZER
    )
  )

-- INV-EVT-07: confirmed registrations must not exceed event capacity
context Event
inv capacityNotExceeded:
  Registration.allInstances()->select(r |
    r.eventId = self.id and
    r.status = RegistrationStatus::CONFIRMED
  )->size() <= self.maxSeats

-- INV-EVT-08: a participant may have only one confirmed registration per event
context Event
inv noDuplicateConfirmedRegistrations:
  Registration.allInstances()->select(r |
    r.eventId = self.id and
    r.status = RegistrationStatus::CONFIRMED
  )->isUnique(r | r.participantId)

-- PRE-EVT-01: event details can be updated only if the event is not cancelled
context Event::updateDetails()
pre notCancelledForUpdate:
  self.status <> EventStatus::CANCELLED

-- PRE-EVT-02: cancelling an event requires that it is not already cancelled
context Event::cancel()
pre notAlreadyCancelled:
  self.status <> EventStatus::CANCELLED

-- POST-EVT-01: after cancel(), the event status becomes CANCELLED
context Event::cancel()
post statusCancelled:
  self.status = EventStatus::CANCELLED

-- PRE-EVT-03: rescheduling requires a non-cancelled event and valid new dates
context Event::reschedule(newStartTime: LocalDateTime, newEndTime: LocalDateTime)
pre validReschedule:
  self.status <> EventStatus::CANCELLED and
  newEndTime > newStartTime

-- POST-EVT-02: after reschedule(), the event status becomes RESCHEDULED
context Event::reschedule(newStartTime: LocalDateTime, newEndTime: LocalDateTime)
post statusRescheduled:
  self.status = EventStatus::RESCHEDULED
```

---

## 4. Class: OrganizerAssignment

```ocl
-- INV-ORG-01: eventId must be a positive identifier
context OrganizerAssignment
inv eventIdPositive:
  self.eventId <> null and self.eventId > 0

-- INV-ORG-02: organizerId must be a positive identifier
context OrganizerAssignment
inv organizerIdPositive:
  self.organizerId <> null and self.organizerId > 0

-- INV-ORG-03: the assigned organizer must have the ORGANIZER role
context OrganizerAssignment
inv organizerRoleValid:
  User.allInstances()->exists(u |
    u.id = self.organizerId and
    u.role.name = RoleName::ORGANIZER
  )

-- INV-ORG-04: the same organizer cannot be assigned to the same event more than once
context OrganizerAssignment
inv noDuplicateOrganizerAssignment:
  OrganizerAssignment.allInstances()->forAll(a1, a2 |
    a1 <> a2 implies not (
      a1.eventId = a2.eventId and
      a1.organizerId = a2.organizerId
    )
  )
```

---

## 5. Class: Registration

```ocl
-- INV-REG-01: eventId must be a positive identifier
context Registration
inv eventIdPositive:
  self.eventId <> null and self.eventId > 0

-- INV-REG-02: participantId must be a positive identifier
context Registration
inv participantIdPositive:
  self.participantId <> null and self.participantId > 0

-- INV-REG-03: registration date must exist
context Registration
inv registeredAtExists:
  self.registeredAt <> null

-- INV-REG-04: active registrations are allowed only for scheduled or rescheduled events
context Registration
inv registrationAllowed:
  self.status = RegistrationStatus::CANCELLED or
  Event.allInstances()->exists(e |
    e.id = self.eventId and
    (e.status = EventStatus::SCHEDULED or
     e.status = EventStatus::RESCHEDULED)
  )

-- INV-REG-05: registration status must be one of the defined literals
context Registration
inv validRegistrationStatus:
  self.status = RegistrationStatus::CONFIRMED or
  self.status = RegistrationStatus::CANCELLED

-- PRE-REG-01: a participant can register only for an available event
context Registration::create(eventId: Long, participantId: Long)
pre eventAvailable:
  Event.allInstances()->exists(e |
    e.id = eventId and
    (e.status = EventStatus::SCHEDULED or
     e.status = EventStatus::RESCHEDULED)
  )

-- PRE-REG-02: a participant can register only if seats are available
context Registration::create(eventId: Long, participantId: Long)
pre seatsAvailable:
  Event.allInstances()->exists(e |
    e.id = eventId and
    Registration.allInstances()->select(r |
      r.eventId = eventId and
      r.status = RegistrationStatus::CONFIRMED
    )->size() < e.maxSeats
  )

-- PRE-REG-03: a participant cannot have another confirmed registration for the same event
context Registration::create(eventId: Long, participantId: Long)
pre notAlreadyRegistered:
  Registration.allInstances()->forAll(r |
    not (
      r.eventId = eventId and
      r.participantId = participantId and
      r.status = RegistrationStatus::CONFIRMED
    )
  )

-- POST-REG-01: after successful registration, a confirmed registration exists
context Registration::create(eventId: Long, participantId: Long)
post registrationCreated:
  Registration.allInstances()->exists(r |
    r.eventId = eventId and
    r.participantId = participantId and
    r.status = RegistrationStatus::CONFIRMED and
    not Registration.allInstances()@pre->includes(r)
  )

-- PRE-REG-04: a registration can be cancelled only if it is confirmed
context Registration::cancel()
pre confirmedBeforeCancel:
  self.status = RegistrationStatus::CONFIRMED

-- POST-REG-02: after cancel(), the registration status becomes CANCELLED
context Registration::cancel()
post statusCancelled:
  self.status = RegistrationStatus::CANCELLED
```

---

## 6. Class: Notification

```ocl
-- INV-NOT-01: notification title must not be empty
context Notification
inv titleNotEmpty:
  self.title <> null and self.title.size() > 0

-- INV-NOT-02: notification message must not be empty
context Notification
inv messageNotEmpty:
  self.message <> null and self.message.size() > 0

-- INV-NOT-03: notification must reference an existing user
context Notification
inv recipientExists:
  User.allInstances()->exists(u |
    u.id = self.userId
  )

-- INV-NOT-04: createdAt must exist
context Notification
inv createdAtExists:
  self.createdAt <> null

-- PRE-NOT-01: markAsRead() requires the notification to be unread
context Notification::markAsRead()
pre notAlreadyRead:
  self.read = false

-- POST-NOT-01: after markAsRead(), the notification becomes read
context Notification::markAsRead()
post markedAsRead:
  self.read = true
```

---

## 7. Service-Level Operation Constraints

```ocl
-- PRE-SRV-01: registerParticipant() requires a unique email
context AuthService::registerParticipant(email: String, password: String, fullName: String)
pre emailUnique:
  not User.allInstances()->exists(u | u.email = email)

-- POST-SRV-01: registerParticipant() creates a participant user
context AuthService::registerParticipant(email: String, password: String, fullName: String)
post participantCreated:
  User.allInstances()->exists(u |
    u.email = email and
    u.role.name = RoleName::PARTICIPANT and
    not User.allInstances()@pre->includes(u)
  )

-- POST-SRV-02: createEvent() creates an event that has at least one organizer assignment
context EventService::createEvent()
post eventHasOrganizer:
  Event.allInstances()->exists(e |
    not Event.allInstances()@pre->includes(e) and
    OrganizerAssignment.allInstances()->exists(a |
      a.eventId = e.id
    )
  )

-- PRE-SRV-02: assignOrganizer() requires the target user to have the ORGANIZER role
context EventService::assignOrganizer(eventId: Long, organizerId: Long)
pre organizerValid:
  User.allInstances()->exists(u |
    u.id = organizerId and
    u.role.name = RoleName::ORGANIZER
  )

-- POST-SRV-03: getUserNotifications() returns only notifications for the requested user
context NotificationService::getUserNotifications(userId: Long)
post onlyUserNotifications:
  result->forAll(n | n.userId = userId)
```

---
