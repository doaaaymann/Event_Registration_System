# Event Registration System Fixes and Build Guide

## What was fixed

This project was updated on `2026-04-26` to address the runtime and architecture issues found during review.

### 1. Config/bootstrap race fixed

- Removed `optional:configserver` from:
  - `auth-service`
  - `event-service`
  - `registration-service`
  - `notification-service`
  - `eureka-server`
  - `api-gateway`
- Added Spring Cloud Config fail-fast retry settings so services wait and retry instead of silently starting with partial local defaults.
- Added `spring-retry` support to all config-client services.
- Fixed local fallback key mismatches in `registration-service` and `notification-service` so their local property names match Docker Compose environment variables.

### 2. Seat availability now fails closed

- `event-service` no longer returns `0` registrations when `registration-service` is unavailable.
- `event-service` now throws a dedicated `DownstreamServiceException`.
- Global exception handling now maps that condition to HTTP `503 Service Unavailable`.
- Runtime verification confirmed this behavior by stopping `registration-service` and calling `GET /api/events/{id}/availability`.

### 3. Registration locking now works across multiple instances

- Replaced the JVM-only `ConcurrentHashMap + ReentrantLock` approach with a PostgreSQL transaction-scoped advisory lock.
- This protects registration creation even if `registration-service` is scaled horizontally.

### 4. Cancel then re-register now works

- Removed the table-level unique constraint that blocked future re-registration after cancellation.
- Added a partial unique index that only prevents duplicate active `REGISTERED` rows.
- Updated duplicate-check logic so only active registrations are treated as duplicates.
- Runtime verification confirmed:
  - first registration succeeds
  - cancellation succeeds
  - re-registration succeeds

### 5. Admin seeding is no longer silently insecure by default

- Added `application.admin.seed.enabled`.
- Removed the hidden default email/password from service config.
- Docker Compose now sets demo admin seed values explicitly instead of relying on hidden defaults.

Important note:
- If you already have an old Docker volume, the previously seeded admin user may still exist with its old password.
- In my live test environment, the existing persisted admin account still used `Admin12345` because the old database volume was reused.
- On a fresh volume created from the current Compose file, the demo password is `EventAdmin123!`.

### 6. Event list/count coupling reduced

- Added a batch registration-count endpoint in `registration-service`.
- `event-service` now fetches registration counts in bulk for event list endpoints instead of making one synchronous count request per event.

### 7. Test stability fixed

- Added test-only configuration in:
  - `registration-service/src/test/resources/application.yml`
  - `notification-service/src/test/resources/application.yml`
- This prevents Web MVC tests from trying to contact the config server during unit/test runs.

## Files added or changed

Main code/config changes include:

- `docker-compose.yml`
- `api-gateway/pom.xml`
- `api-gateway/src/main/resources/application.yml`
- `auth-service/pom.xml`
- `auth-service/src/main/java/com/event/authservice/config/AdminSeedProperties.java`
- `auth-service/src/main/java/com/event/authservice/config/AdminSeeder.java`
- `auth-service/src/main/resources/application.yml`
- `config-server/src/main/resources/config/auth-service.yml`
- `event-service/pom.xml`
- `event-service/src/main/java/com/event/eventservice/client/RegistrationServiceClient.java`
- `event-service/src/main/java/com/event/eventservice/exception/DownstreamServiceException.java`
- `event-service/src/main/java/com/event/eventservice/exception/GlobalExceptionHandler.java`
- `event-service/src/main/java/com/event/eventservice/service/EventService.java`
- `event-service/src/main/resources/application.yml`
- `event-service/src/test/java/com/event/eventservice/service/EventServiceTest.java`
- `eureka-server/pom.xml`
- `eureka-server/src/main/resources/application.yml`
- `notification-service/pom.xml`
- `notification-service/src/main/resources/application.yml`
- `notification-service/src/test/resources/application.yml`
- `registration-service/pom.xml`
- `registration-service/src/main/java/com/event/registrationservice/controller/RegistrationQueryController.java`
- `registration-service/src/main/java/com/event/registrationservice/dto/response/RegistrationCountProjection.java`
- `registration-service/src/main/java/com/event/registrationservice/entity/Registration.java`
- `registration-service/src/main/java/com/event/registrationservice/repository/RegistrationRepository.java`
- `registration-service/src/main/java/com/event/registrationservice/service/EventLockManager.java`
- `registration-service/src/main/java/com/event/registrationservice/service/RegistrationQueryService.java`
- `registration-service/src/main/java/com/event/registrationservice/service/RegistrationService.java`
- `registration-service/src/main/resources/application.yml`
- `registration-service/src/main/resources/db/migration/V20260426_220000__allow_reregistration_and_active_unique_index.sql`
- `registration-service/src/test/java/com/event/registrationservice/service/RegistrationQueryServiceTest.java`
- `registration-service/src/test/java/com/event/registrationservice/service/RegistrationServiceTest.java`
- `registration-service/src/test/resources/application.yml`

## Build steps

### Option 1. Build everything with Docker Compose

From the project root:

```powershell
docker compose build
```

Expected result:

- All service images build successfully.
- Final lines include `Built` for:
  - `config-server`
  - `eureka-server`
  - `api-gateway`
  - `auth-service`
  - `event-service`
  - `registration-service`
  - `notification-service`

### Option 2. Run the Maven tests inside Docker

This is useful when Maven is not installed locally.

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.6-eclipse-temurin-17 mvn -B -ntp test
```

Expected result:

- Maven finishes with `BUILD SUCCESS`.
- The tested modules pass:
  - `auth-service`
  - `event-service`
  - `registration-service`
  - `notification-service`

My verified result:

- Full suite status: `BUILD SUCCESS`
- Total tested service cases:
  - `auth-service`: `15`
  - `event-service`: `22`
  - `registration-service`: `17`
  - `notification-service`: `11`

### Option 3. Start the full stack

```powershell
docker compose up -d
```

Expected result:

- All containers start.
- `docker compose ps` shows:
  - `config-server`
  - `eureka-server`
  - `api-gateway`
  - `auth-service`
  - `event-service`
  - `registration-service`
  - `notification-service`
  - `postgres`

## Runtime verification I performed

I verified the project myself with these real checks:

1. `docker compose build`
   - Result: success

2. `mvn test` executed inside Docker
   - Result: success

3. Full stack startup with `docker compose up -d`
   - Result: success

4. Health checks for every service
   - Result: all `UP`

5. End-to-end business flow
   - Admin login
   - Participant registration
   - Participant login
   - Event creation
   - Event availability check
   - Registration creation
   - Registration cancellation
   - Re-registration
   - Registration count check
   - Notification retrieval
   - Result: success

6. Gateway flow
   - Login through gateway
   - List events through gateway
   - Check availability through gateway
   - Result: success

7. Fail-closed resilience check
   - Stopped `registration-service`
   - Called `event-service` availability endpoint
   - Result: HTTP `503` with message:
     - `Registration count is unavailable for event 6`

## Recommended clean-start note

If you want a completely fresh demo database, remove the old Docker volume first:

```powershell
docker compose down -v
docker compose up -d --build
```

Use this carefully because it deletes the persisted PostgreSQL data for the project.

For step-by-step API testing with expected outputs, use [DOCKER_TESTING_GUIDE.md](C:\Users\asus\OneDrive\Documents\GitHub\Event_Registration_System\DOCKER_TESTING_GUIDE.md).
