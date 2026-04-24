# Registration Service Work Report

## 1. What I analyzed

- The `registration-service` module was almost empty and contained only:
  - `RegistrationServiceApplication`
  - `application.yml`
  - one Flyway migration creating a basic `registrations` table
- The parent Maven build in `pom.xml` defines Spring Boot `3.2.5`, Spring Cloud `2023.0.1`, and Java `17`.
- `auth-service` already had the project's established patterns for:
  - JWT parsing
  - stateless Spring Security
  - unified JSON error responses
- `event-service` and `notification-service` currently expose no implementation code in the workspace, so the integration contract had to be taken from the task specification and the planning documents under `tasks/`.
- `tasks/TEAM_IMPLEMENTATION_PLAN.md` was used to confirm:
  - event response shape
  - availability response shape
  - registration response shape
  - notification request shape

## 2. What I built

I transformed `registration-service` into a complete microservice with:

- JPA entity and repository for registrations
- service layer with business validations
- REST controller with the exact required endpoints
- Flyway-managed schema improvements
- JWT-based stateless security
- Feign clients for `event-service` and `notification-service`
- unified exception handling and error payloads
- unit tests for service logic
- controller tests with mocked service behavior
- JWT filter test to validate claim extraction

## 3. File-by-file changes

### Updated existing files

- `registration-service/pom.xml`
  - added Spring Security
  - added OpenFeign
  - added JJWT dependencies
  - added `spring-security-test`

- `registration-service/src/main/java/com/event/registrationservice/RegistrationServiceApplication.java`
  - enabled Feign clients with `@EnableFeignClients`

- `registration-service/src/main/resources/application.yml`
  - added datasource defaults
  - added JPA validation mode
  - enabled Flyway
  - added Eureka URL configuration
  - added Feign client timeout configuration
  - added JWT secret property binding

- `registration-service/src/main/resources/db/migration/V20260416_203700__init_registration_schema.sql`
  - added unique constraint on `(event_id, participant_id)`
  - added indexes for participant, event, and event/status lookups

### Added main source files

- `registration-service/src/main/java/com/event/registrationservice/client/EventServiceClient.java`
- `registration-service/src/main/java/com/event/registrationservice/client/NotificationServiceClient.java`
- `registration-service/src/main/java/com/event/registrationservice/config/SecurityConfig.java`
- `registration-service/src/main/java/com/event/registrationservice/controller/RegistrationController.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/client/EventAvailabilityResponse.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/client/EventDetailsResponse.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/client/NotificationCommand.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/request/CreateRegistrationRequest.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/response/RegistrationCountResponse.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/response/RegistrationResponse.java`
- `registration-service/src/main/java/com/event/registrationservice/entity/Registration.java`
- `registration-service/src/main/java/com/event/registrationservice/entity/RegistrationStatus.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/ApiErrorResponse.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/BadRequestException.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/ConflictException.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/DownstreamServiceException.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/ForbiddenOperationException.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/GlobalExceptionHandler.java`
- `registration-service/src/main/java/com/event/registrationservice/exception/ResourceNotFoundException.java`
- `registration-service/src/main/java/com/event/registrationservice/repository/RegistrationRepository.java`
- `registration-service/src/main/java/com/event/registrationservice/security/AuthenticatedUser.java`
- `registration-service/src/main/java/com/event/registrationservice/security/JwtAuthenticationFilter.java`
- `registration-service/src/main/java/com/event/registrationservice/security/JwtProperties.java`
- `registration-service/src/main/java/com/event/registrationservice/security/JwtService.java`
- `registration-service/src/main/java/com/event/registrationservice/security/RestAccessDeniedHandler.java`
- `registration-service/src/main/java/com/event/registrationservice/security/RestAuthenticationEntryPoint.java`
- `registration-service/src/main/java/com/event/registrationservice/service/EventLockManager.java`
- `registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java`

### Added test files

- `registration-service/src/test/java/com/event/registrationservice/controller/RegistrationControllerTest.java`
- `registration-service/src/test/java/com/event/registrationservice/controller/TestAuthenticationFactory.java`
- `registration-service/src/test/java/com/event/registrationservice/security/JwtAuthenticationFilterTest.java`
- `registration-service/src/test/java/com/event/registrationservice/service/RegistrationServiceTest.java`

### Post-implementation test fixes

- `registration-service/src/test/java/com/event/registrationservice/controller/RegistrationControllerTest.java`
  - mocked `JwtAuthenticationFilter` so `@WebMvcTest` does not fail while building the sliced MVC context
  - replaced mixed raw values and Mockito matchers with consistent matcher usage
  - allowed nullable authentication principal matching in controller-level tests

- `registration-service/src/test/java/com/event/registrationservice/service/RegistrationServiceTest.java`
  - removed unnecessary lock stubbing from tests that fail before the lock is reached
  - narrowed `EventLockManager` stubbing to only tests that actually execute registration creation
  - corrected the "no seats available" test fixture so it represents full capacity instead of closed registration

## 4. Endpoint explanations

### `POST /api/registrations`

- Creates a new registration for the authenticated user.
- Reads `userId` and `roles` from JWT-derived security context.
- Validates:
  - authenticated user exists
  - role contains `PARTICIPANT`
  - no duplicate registration
  - event exists
  - event status is `SCHEDULED`
  - availability says seats remain
- Saves a row with status `REGISTERED`
- Calls notification endpoint for registration creation

### `GET /api/registrations/{registrationId}`

- Returns one registration
- Restricted to the registration owner

### `GET /api/registrations/me`

- Returns all registrations for the authenticated user
- Includes both `REGISTERED` and `CANCELLED`

### `GET /api/registrations/events/{eventId}`

- Returns all registrations stored for one event
- Verifies the event exists first via `event-service`

### `DELETE /api/registrations/{registrationId}`

- Cancels the authenticated user's registration
- Does not delete the database row
- Updates status to `CANCELLED`
- Sets `cancelledAt`
- Calls notification endpoint for cancellation

### `GET /api/registrations/events/{eventId}/count`

- Returns the number of registrations for the event with status `REGISTERED`
- Excludes cancelled rows

## 5. Business rules explanation

### Rule 1: no duplicate registration

- Enforced by:
  - repository existence check before insert
  - database unique constraint on `(event_id, participant_id)`

### Rule 2: no overbooking

- Enforced by `event-service` availability check before save
- Also serialized locally per event using `EventLockManager`

### Rule 3: only `PARTICIPANT` can register

- Enforced in service layer after JWT extraction

### Rule 4: only owner can cancel

- Enforced by comparing registration `participantId` with JWT `userId`

### Rule 5: event must be `SCHEDULED`

- Enforced using event details and availability responses

### Rule 6: cancellation is status change only

- The record is preserved
- `status` becomes `CANCELLED`
- `cancelledAt` is stored

## 6. Overbooking prevention logic

The implemented sequence is:

1. Acquire an in-memory lock for the target `eventId`
2. Check duplicate registration
3. Call `GET /api/events/{eventId}`
4. Ensure event status is `SCHEDULED`
5. Call `GET /api/events/{eventId}/availability`
6. Ensure:
   - status is `SCHEDULED`
   - `registrationOpen != false`
   - `availableSeats > 0`
7. Save registration

Why the lock was added:

- Without a lock, two concurrent requests inside the same service instance could both pass availability check before either insert completes.
- The lock serializes registration creation per event within the running instance.

Important limitation:

- This protects concurrency within one service instance.
- True multi-instance distributed overbooking prevention would require a stronger cross-service reservation/seat-consumption contract from `event-service`, which does not exist in the current workspace contract.

## 7. JWT authentication handling

Security implementation mirrors the project style already used in `auth-service`:

- `JwtAuthenticationFilter`
  - reads `Authorization: Bearer <token>`
  - parses claims using shared JWT secret
  - extracts:
    - `userId`
    - `roles`
    - fallback support for `role` and `authorities`
- `AuthenticatedUser`
  - stores `userId`, username/subject, and normalized roles
- `SecurityConfig`
  - stateless session policy
  - all registration endpoints require authentication
  - actuator health/info remain public

## 8. Integration flow with other services

### Registration creation flow

1. Client calls `POST /api/registrations`
2. JWT filter extracts authenticated user
3. `registration-service` calls `event-service`:
   - `GET /api/events/{eventId}`
   - `GET /api/events/{eventId}/availability`
4. If valid, registration is stored locally
5. `registration-service` calls `notification-service`:
   - `POST /api/notifications/internal/registration-created`

### Registration cancellation flow

1. Client calls `DELETE /api/registrations/{registrationId}`
2. Ownership is validated from JWT user
3. Registration status becomes `CANCELLED`
4. `registration-service` calls `notification-service`:
   - `POST /api/notifications/internal/registration-cancelled`

### Notification payload used

Because the workspace did not contain notification endpoint body classes, the implementation used the already documented minimal request shape from the planning document:

```json
{
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

and for cancellation:

```json
{
  "userId": 1,
  "type": "REGISTRATION_CANCELLED",
  "title": "Registration Cancelled",
  "message": "Your registration was cancelled for Spring Boot Workshop"
}
```

## 9. Postman testing steps

### Prerequisites

1. Start:
   - `config-server`
   - `eureka-server`
   - `auth-service`
   - `event-service`
   - `notification-service`
   - `registration-service`
2. Ensure PostgreSQL database for `registration-service` is available
3. Ensure config values or environment variables are set:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `JWT_SECRET`
   - `CONFIG_SERVER_URI` if not localhost
   - `EUREKA_URI` if not localhost

### Step A: authenticate and obtain JWT

1. Use `auth-service` login endpoint
2. Copy the returned access token
3. In Postman create an environment variable:
   - `jwt_token`
4. Set Authorization header for protected requests:
   - `Authorization: Bearer {{jwt_token}}`

### Step B: create or identify a scheduled event

1. Call `GET /api/events/{eventId}`
2. Confirm response contains:
   - `status = SCHEDULED`
3. Call `GET /api/events/{eventId}/availability`
4. Confirm:
   - `availableSeats > 0`
   - `registrationOpen = true` if field exists

### Step C: create a registration

Request:

```http
POST /api/registrations
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

Body:

```json
{
  "eventId": 10
}
```

Expected response:

- HTTP `201 Created`
- body similar to:

```json
{
  "id": 100,
  "eventId": 10,
  "participantId": 1,
  "status": "REGISTERED",
  "registeredAt": "2026-04-24T09:30:00",
  "cancelledAt": null
}
```

### Step D: verify duplicate protection

1. Send the same `POST /api/registrations` request again
2. Expected response:
   - HTTP `409 Conflict`
   - body:

```json
{
  "timestamp": "2026-04-24 09:31:00",
  "status": 409,
  "error": "Conflict",
  "message": "User is already registered for this event",
  "path": "/api/registrations"
}
```

### Step E: fetch current user registrations

Request:

```http
GET /api/registrations/me
Authorization: Bearer {{jwt_token}}
```

Expected response:

- HTTP `200 OK`
- array of registrations for the authenticated user

### Step F: fetch a registration by id

Request:

```http
GET /api/registrations/100
Authorization: Bearer {{jwt_token}}
```

Expected response:

- HTTP `200 OK` for owner
- HTTP `403 Forbidden` for another user

### Step G: fetch event registrations

Request:

```http
GET /api/registrations/events/10
Authorization: Bearer {{jwt_token}}
```

Expected response:

- HTTP `200 OK`
- array of registrations for event `10`

### Step H: fetch active count

Request:

```http
GET /api/registrations/events/10/count
Authorization: Bearer {{jwt_token}}
```

Expected response:

```json
{
  "eventId": 10,
  "registeredCount": 1
}
```

### Step I: cancel a registration

Request:

```http
DELETE /api/registrations/100
Authorization: Bearer {{jwt_token}}
```

Expected response:

```json
{
  "id": 100,
  "eventId": 10,
  "participantId": 1,
  "status": "CANCELLED",
  "registeredAt": "2026-04-24T09:30:00",
  "cancelledAt": "2026-04-24T09:40:00"
}
```

### Step J: verify count after cancellation

1. Repeat `GET /api/registrations/events/10/count`
2. Expected `registeredCount` decreases because only `REGISTERED` rows are counted

### Step K: verify role restriction

1. Login as a non-`PARTICIPANT` user
2. Call `POST /api/registrations`
3. Expected response:
   - HTTP `403 Forbidden`

## 10. Example requests and responses

### Create registration request

```json
{
  "eventId": 10
}
```

### Create registration response

```json
{
  "id": 100,
  "eventId": 10,
  "participantId": 1,
  "status": "REGISTERED",
  "registeredAt": "2026-04-24T09:30:00",
  "cancelledAt": null
}
```

### Count response

```json
{
  "eventId": 10,
  "registeredCount": 35
}
```

### Standard error response

```json
{
  "timestamp": "2026-04-24 09:31:00",
  "status": 400,
  "error": "Bad Request",
  "message": "No seats available for this event",
  "path": "/api/registrations"
}
```

## 11. Problems encountered and solutions

### Problem: `registration-service` was only a skeleton

- Solution:
  - built all required application layers from scratch within the module

### Problem: no concrete workspace implementation existed for `event-service` and `notification-service`

- Solution:
  - used the strict endpoint contract from the task
  - used the planning documents under `tasks/` to avoid inventing broader payloads

### Problem: duplicate protection required both application and database safety

- Solution:
  - added repository pre-check
  - added DB unique constraint

### Problem: overbooking prevention can race under concurrent requests

- Solution:
  - serialized registration creation per event inside the service instance with `EventLockManager`
  - still relied on `event-service` availability as the authority for seat availability

### Problem: initial test suite failed during verification

- Solution:
  - identified a sliced MVC test context issue caused by `JwtAuthenticationFilter` dependency resolution in `@WebMvcTest`
  - fixed Mockito matcher misuse in controller tests
  - removed unnecessary stubbing in service tests
  - corrected a service test fixture that was asserting the wrong validation branch

## 12. Verification status

- Executed:
  - `mvn -pl registration-service test`
- Result:
  - `BUILD SUCCESS`
  - `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`
- Notes:
  - controller tests, service tests, and JWT filter tests all pass
  - verification was completed on `2026-04-24`
