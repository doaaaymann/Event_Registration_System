# Notification Service Work Report

## 1. What I analyzed

- The `notification-service` module was initially a skeleton and contained only:
  - `NotificationServiceApplication`
  - `application.yml`
  - one Flyway migration creating a basic `notifications` table
- The parent Maven build in `pom.xml` defines Spring Boot `3.2.5`, Spring Cloud `2023.0.1`, and Java `17`.
- `registration-service` already showed the established project patterns for:
  - stateless JWT authentication
  - unified JSON error responses
  - layered structure with controller, service, repository, DTOs, and tests
- `tasks/TEAM_IMPLEMENTATION_PLAN.md` was used to confirm:
  - required notification endpoints
  - notification database ownership
  - dependency on registration and event integration triggers
- `tasks/REGISTRATION_SERVICE_POSTMAN_TESTING.md` and `tasks/Registration_Service_Work_Report.md` were used as style references for the final documentation files.

## 2. What I built

I transformed `notification-service` into a complete microservice with:

- JPA entity and repository for notifications
- service layer covering creation, retrieval, read updates, and internal triggers
- REST controller exposing the exact required endpoints
- Flyway-managed schema improvements
- JWT-based stateless security
- unified exception handling and error payloads
- unit tests for service logic
- controller tests with mocked service behavior
- JWT filter test to validate claim extraction

After the first notification-service build, I also updated `event-service` so event reschedule and cancellation now trigger notifications for registered participants through `notification-service`.

## 3. File-by-file changes

### Updated existing files

- `notification-service/pom.xml`
  - added Spring Security
  - added JJWT dependencies
  - added `spring-security-test`

- `notification-service/src/main/resources/application.yml`
  - added local datasource defaults
  - added JPA validation mode
  - enabled Flyway
  - added Eureka URL configuration
  - added JWT secret property fallback

- `notification-service/src/main/resources/db/migration/V20260416_203800__init_notification_schema.sql`
  - kept the required `notifications` table
  - added indexes for user and read lookups

### Added main source files

- `notification-service/src/main/java/com/event/notificationservice/config/SecurityConfig.java`
- `notification-service/src/main/java/com/event/notificationservice/controller/NotificationController.java`
- `notification-service/src/main/java/com/event/notificationservice/dto/request/CreateNotificationRequest.java`
- `notification-service/src/main/java/com/event/notificationservice/dto/request/InternalNotificationTriggerRequest.java`
- `notification-service/src/main/java/com/event/notificationservice/dto/response/NotificationResponse.java`
- `notification-service/src/main/java/com/event/notificationservice/entity/Notification.java`
- `notification-service/src/main/java/com/event/notificationservice/exception/ApiErrorResponse.java`
- `notification-service/src/main/java/com/event/notificationservice/exception/BadRequestException.java`
- `notification-service/src/main/java/com/event/notificationservice/exception/ForbiddenOperationException.java`
- `notification-service/src/main/java/com/event/notificationservice/exception/GlobalExceptionHandler.java`
- `notification-service/src/main/java/com/event/notificationservice/exception/ResourceNotFoundException.java`
- `notification-service/src/main/java/com/event/notificationservice/repository/NotificationRepository.java`
- `notification-service/src/main/java/com/event/notificationservice/security/AuthenticatedUser.java`
- `notification-service/src/main/java/com/event/notificationservice/security/JwtAuthenticationFilter.java`
- `notification-service/src/main/java/com/event/notificationservice/security/JwtProperties.java`
- `notification-service/src/main/java/com/event/notificationservice/security/JwtService.java`
- `notification-service/src/main/java/com/event/notificationservice/security/RestAccessDeniedHandler.java`
- `notification-service/src/main/java/com/event/notificationservice/security/RestAuthenticationEntryPoint.java`
- `notification-service/src/main/java/com/event/notificationservice/service/NotificationService.java`

### Added test files

- `notification-service/src/test/java/com/event/notificationservice/controller/NotificationControllerTest.java`
- `notification-service/src/test/java/com/event/notificationservice/controller/TestAuthenticationFactory.java`
- `notification-service/src/test/java/com/event/notificationservice/security/JwtAuthenticationFilterTest.java`
- `notification-service/src/test/java/com/event/notificationservice/service/NotificationServiceTest.java`

### Additional cross-service updates after notification implementation

- `event-service/src/main/java/com/event/eventservice/config/ClientConfig.java`
  - added Authorization header forwarding for downstream requests

- `event-service/src/main/java/com/event/eventservice/client/RegistrationServiceClient.java`
  - added event participant retrieval from `registration-service`

- `event-service/src/main/java/com/event/eventservice/client/NotificationServiceClient.java`
  - added calls to notification internal endpoints

- `event-service/src/main/java/com/event/eventservice/dto/client/NotificationTriggerRequest.java`
  - added trigger payload DTO for event notifications

- `event-service/src/main/java/com/event/eventservice/dto/client/RegistrationSummaryResponse.java`
  - added participant DTO for event participant lookup

- `event-service/src/main/java/com/event/eventservice/service/EventService.java`
  - added notification sending during event reschedule and cancel actions

- `event-service/src/test/java/com/event/eventservice/service/EventServiceTest.java`
  - added verification of event-service notification fan-out behavior

## 4. Endpoint explanations

### `POST /api/notifications`

- Creates a notification row for a target user.
- Requires authentication.
- `ADMIN` may create notifications for any user.
- non-admin users may create notifications only for themselves.

### `GET /api/notifications/users/{userId}`

- Returns notifications for the requested user.
- Requires authentication.
- accessible by the owner or `ADMIN`.

### `PATCH /api/notifications/{notificationId}/read`

- Marks one notification as read.
- Requires authentication.
- accessible by the owner or `ADMIN`.

### `POST /api/notifications/internal/registration-created`

- Creates notification rows from the registration-created trigger.
- compatible with the existing `registration-service` request shape.

### `POST /api/notifications/internal/registration-cancelled`

- Creates notification rows from the registration-cancelled trigger.
- compatible with the existing `registration-service` request shape.

### `POST /api/notifications/internal/event-cancelled`

- Creates notification rows for event cancellation.
- supports one recipient through `userId` or many recipients through `userIds`.
- now used by `event-service` when an event is cancelled.

### `POST /api/notifications/internal/event-rescheduled`

- Creates notification rows for event reschedule.
- supports one recipient through `userId` or many recipients through `userIds`.
- now used by `event-service` when an event is rescheduled.

## 5. Business rules explanation

### Rule 1: the service owns only notification data

- all notification rows are stored only in `notification_db`
- the service does not read another service database directly

### Rule 2: owners can read their own notifications

- a normal authenticated user can fetch only their own notification list

### Rule 3: admins can access any user's notifications

- `ADMIN` can create notifications for any user
- `ADMIN` can read or mark read for any user

### Rule 4: internal triggers can fan out to multiple recipients

- internal trigger payloads support:
  - `userId`
  - `userIds`
- duplicate recipient ids are removed before inserts

### Rule 5: marking as read is idempotent

- if a notification is already read, the endpoint still returns the existing row
- the service does not create duplicate read updates

## 6. Database design

The implemented `notifications` table contains:

- `id`
- `user_id`
- `type`
- `title`
- `message`
- `read`
- `created_at`

Added indexes:

- `(user_id, created_at desc)`
- `(user_id, read)`

These support the main query and read-state access pattern required by the service.

## 7. JWT authentication handling

Security implementation follows the pattern already used in `registration-service`:

- `JwtAuthenticationFilter`
  - reads `Authorization: Bearer <token>`
  - parses claims using the shared JWT secret
  - extracts:
    - `userId`
    - `roles`
    - fallback support for `role` and `authorities`
- `AuthenticatedUser`
  - stores `userId`, username, and normalized roles
- `SecurityConfig`
  - stateless session policy
  - all notification endpoints require authentication
  - actuator health/info remain public

## 8. Integration flow with other services

### Registration created flow

1. `registration-service` calls `POST /api/notifications/internal/registration-created`
2. `notification-service` accepts the payload
3. one notification row is inserted for the target user

### Registration cancelled flow

1. `registration-service` calls `POST /api/notifications/internal/registration-cancelled`
2. `notification-service` accepts the payload
3. one notification row is inserted for the target user

### Event cancelled flow

1. `event-service` calls `POST /api/notifications/internal/event-cancelled`
2. the payload can include `userId` or `userIds`
3. one row is created per resolved recipient

### Event rescheduled flow

1. `event-service` calls `POST /api/notifications/internal/event-rescheduled`
2. the payload can include `userId` or `userIds`
3. one row is created per resolved recipient

### Real event-service integration added after the first notification build

1. `event-service` receives `PATCH /api/events/{eventId}/reschedule`
2. `event-service` loads active participants from `registration-service`
3. `event-service` sends `POST /api/notifications/internal/event-rescheduled`
4. `notification-service` inserts one notification row per participant

1. `event-service` receives `PATCH /api/events/{eventId}/cancel`
2. `event-service` loads active participants from `registration-service`
3. `event-service` sends `POST /api/notifications/internal/event-cancelled`
4. `notification-service` inserts one notification row per participant

## 9. Example request and response shapes

### Public create notification request

```json
{
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

### Public create notification response

```json
{
  "id": 501,
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop",
  "read": false,
  "createdAt": "2026-04-26T16:20:00"
}
```

### List notifications response

```json
[
  {
    "id": 501,
    "userId": 1,
    "type": "REGISTRATION_CONFIRMED",
    "title": "Registration Confirmed",
    "message": "You are registered for Spring Boot Workshop",
    "read": false,
    "createdAt": "2026-04-26T16:20:00"
  }
]
```

### Internal event trigger request

```json
{
  "userIds": [1, 2, 3],
  "type": "EVENT_RESCHEDULED",
  "title": "Event Rescheduled",
  "message": "Spring Boot Workshop has been rescheduled"
}
```

### Standard error response

```json
{
  "timestamp": "2026-04-26T16:25:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Only the notification owner or ADMIN can perform this action",
  "path": "/api/notifications/users/2"
}
```

## 10. Problems encountered and solutions

### Problem: `notification-service` was only a skeleton

- Solution:
  - built all required application layers from scratch within the module

### Problem: event and registration trigger payloads were only partially specified in the planning notes

- Solution:
  - kept strict compatibility with the current registration payload shape
  - added support for `userIds` on event triggers so `event-service` can notify many users without reading another service database

### Problem: notification access had to be secure without introducing shared DB reads

- Solution:
  - enforced owner-or-admin authorization in the notification service itself
  - used JWT claims only for identity and role checks

### Problem: the service needed to stay aligned with central config without changing shared platform files

- Solution:
  - added local fallback settings in `notification-service/application.yml`
  - left platform-owned config files untouched

### Problem: event-service originally had no caller for event-cancelled and event-rescheduled notification endpoints

- Solution:
  - added participant lookup from `registration-service`
  - added downstream notification calls from `event-service`
  - forwarded Authorization headers from `event-service` to downstream services

### Problem: Docker startup order caused temporary datasource failures in `event-service` and `registration-service`

- Solution:
  - confirmed this was a startup timing issue with `config-server`
  - restarting those services after `config-server` was ready fixed the issue:

```powershell
docker compose restart event-service registration-service
```

## 11. Verification status

- Code review status:
  - implementation completed for all required Person 4 endpoints and database ownership rules
  - request and response shapes were aligned to the team planning document
  - registration trigger endpoints were aligned to the current `registration-service` Feign client contract
- Docker verification status:
  - `notification-service` built successfully in Docker
  - `auth-service` login was verified
  - notification create, read, and mark-read endpoints were verified
  - internal event trigger endpoints were verified
  - `401` and `403` error behavior were verified
- Cross-microservice verification status:
  - `registration-service` -> `notification-service` was verified through real registration creation
  - `event-service` -> `notification-service` was implemented and verified after the update
  - real end-to-end flow verified:
    1. create event
    2. register two participants
    3. reschedule event
    4. cancel event
    5. confirm notifications for both participants

Verified example outputs from the real Docker test:

Event created:

```json
{
  "id": 1,
  "title": "Integration Test Event",
  "status": "SCHEDULED",
  "availableSeats": 50
}
```

Event rescheduled:

```json
{
  "id": 1,
  "title": "Integration Test Event",
  "status": "RESCHEDULED",
  "registeredCount": 2,
  "availableSeats": 48
}
```

Event cancelled:

```json
{
  "id": 1,
  "title": "Integration Test Event",
  "status": "CANCELLED",
  "registeredCount": 2,
  "availableSeats": 48
}
```

Verified notification entries for registered users included:

```json
{
  "type": "EVENT_RESCHEDULED",
  "title": "Event Rescheduled",
  "message": "Integration Test Event has been rescheduled"
}
```

```json
{
  "type": "EVENT_CANCELLED",
  "title": "Event Cancelled",
  "message": "Integration Test Event was cancelled"
}
```
