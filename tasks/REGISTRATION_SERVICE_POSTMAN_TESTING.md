# Registration Service Work And Testing Guide

This guide is for Person 3, the owner of `registration-service`.

It explains:

- how to open the project
- how to run only the services needed for registration testing
- how to work inside your part safely
- how to run the registration tests
- how to test the registration APIs in Postman
- how to decide that your part is finished

## 1. Open The Project

Open a terminal in the project root and move into the repository:

```powershell
cd C:\Users\doaaa\Documents\GitHub\Event_Registration_System-
```

To confirm you are in the correct place:

```powershell
dir
```

You should see folders like:

- `auth-service`
- `api-gateway`
- `config-server`
- `eureka-server`
- `event-service`
- `registration-service`
- `notification-service`
- `tasks`

## 2. What Person 3 Owns

Person 3 owns only the registration microservice.

You should mainly work inside:

- `registration-service/src/main/java`
- `registration-service/src/main/resources`
- `registration-service/src/test/java`
- `registration-service/pom.xml`

Avoid editing:

- `auth-service`
- `event-service`
- `notification-service`
- `api-gateway`
- `config-server`
- `eureka-server`
- `docker-compose.yml`

unless the team agrees first.

## 3. What Person 3 Is Responsible For

According to the task plan, Person 3 is responsible for:

- attendee registration
- registration cancellation
- participant tracking
- registration count
- overbooking prevention inside registration flow
- registration database tables
- registration tests

## 4. Run Only The Registration Stack With Docker

If you only want the services needed for registration testing, run:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service
```

This starts:

- PostgreSQL
- Config Server
- Eureka Server
- Auth Service
- Event Service
- Registration Service

`notification-service` is optional for Person 3 phase testing.
Registration creation/cancellation should still succeed if notification is not running.

To stop the containers:

```powershell
docker compose down
```

To watch only registration-service logs:

```powershell
docker compose logs -f registration-service
```

## 5. Useful URLs

Direct registration-service base URL:

```text
http://localhost:8083
```

If later you want to test through the API Gateway:

```text
http://localhost:8080
```

For Person 3 testing, use the direct registration-service URL first:

```text
http://localhost:8083
```

## 6. How To Work On Your Part

Before changing code:

1. go to the root project folder
2. make sure the registration stack can run
3. work only inside `registration-service`
4. after changes, run registration tests
5. then test manually in Postman

Recommended local workflow:

```powershell
cd C:\Users\doaaa\Documents\GitHub\Event_Registration_System-
```

```powershell
mvn -pl registration-service test
```

If tests pass, run the registration stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service
```

Then verify the endpoints in Postman.

## 7. How To Run The Registration Tests

Run all registration-service tests:

```powershell
mvn -pl registration-service test
```

Run only service tests:

```powershell
mvn -pl registration-service -Dtest=RegistrationServiceTest test
```

Run only controller tests:

```powershell
mvn -pl registration-service -Dtest=RegistrationControllerTest test
```

Run only JWT filter test:

```powershell
mvn -pl registration-service -Dtest=JwtAuthenticationFilterTest test
```

Expected result:

- build success
- all registration tests pass

## 8. Required Service Contracts

Person 3 depends on these external endpoints:

From `event-service`:

- `GET /api/events/{eventId}`
- `GET /api/events/{eventId}/availability`

From `notification-service` (Phase 5 integration, optional for Person 3 completion):

- `POST /api/notifications/internal/registration-created`
- `POST /api/notifications/internal/registration-cancelled`

## 9. Postman Setup

Create a Postman environment and add these variables:

- `registrationBaseUrl = http://localhost:8083`
- `authBaseUrl = http://localhost:8081`
- `participantToken =`
- `adminToken =`
- `eventId =`
- `registrationId =`

## 10. Auth Note For Role Testing

- Public `POST /api/auth/register` allows `PARTICIPANT` only.
- For admin role checks, login with the seeded admin account:

```json
{
  "email": "admin@event.local",
  "password": "Admin12345"
}
```

- These are default values from `config-server/src/main/resources/config/auth-service.yml` and can be overridden by environment variables.

## 11. Important Postman Rules

- Always use `{{registrationBaseUrl}}` for registration requests
- Use `{{authBaseUrl}}` only for login
- For protected endpoints, send `Authorization: Bearer {{participantToken}}`
- Use `/registrations/100`, not `/registrations/{100}`
- Test `registration-service` directly before testing through the gateway

## 12. Postman Test Flow

### Request 1: Login Participant

- Method: `POST`
- URL: `{{authBaseUrl}}/api/auth/login`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "email": "ali@example.com",
  "password": "Secret123"
}
```

Expected:

- `200 OK`

Example response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "userId": 2,
  "email": "ali@example.com",
  "roles": ["PARTICIPANT"]
}
```

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("participantToken", json.accessToken);
```

### Request 2: Login Admin

- Method: `POST`
- URL: `{{authBaseUrl}}/api/auth/login`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "email": "admin@event.local",
  "password": "Admin12345"
}
```

Expected:

- `200 OK`
- token includes role `ADMIN`

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("adminToken", json.accessToken);
```

### Request 3: Confirm Event Exists

- Method: `GET`
- URL: `http://localhost:8082/api/events/10`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Expected response must include:

- `id`
- `title`
- `status`

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("eventId", json.id);
```

### Request 4: Confirm Event Availability

- Method: `GET`
- URL: `http://localhost:8082/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Expected response should show:

- `status = SCHEDULED`
- `availableSeats > 0`
- `registrationOpen = true`

### Request 5: Create Registration

- Method: `POST`
- URL: `{{registrationBaseUrl}}/api/registrations`

Headers:

- `Authorization: Bearer {{participantToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "eventId": {{eventId}}
}
```

Expected:

- `201 Created`

Example response:

```json
{
  "id": 100,
  "eventId": 10,
  "participantId": 2,
  "status": "REGISTERED",
  "registeredAt": "2026-04-24T15:30:00",
  "cancelledAt": null
}
```

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("registrationId", json.id);
```

### Request 6: Reject Duplicate Registration

- Method: `POST`
- URL: `{{registrationBaseUrl}}/api/registrations`

Headers:

- `Authorization: Bearer {{participantToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "eventId": {{eventId}}
}
```

Expected:

- `409 Conflict`

Expected message:

```json
{
  "message": "User is already registered for this event"
}
```

### Request 7: Get My Registrations

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/me`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- array contains the registration created above

### Request 8: Get Registration By Id

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/{{registrationId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

### Request 9: Get Event Registration Count

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/events/{{eventId}}/count`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Example response:

```json
{
  "eventId": 10,
  "registeredCount": 1
}
```

### Request 10: Participant Tracking Access Control

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/events/{{eventId}}`

Headers (participant):

- `Authorization: Bearer {{participantToken}}`

Expected:

- `403 Forbidden`

Headers (admin):

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`
- list of registrations for the event

### Request 11: Cancel Registration

- Method: `DELETE`
- URL: `{{registrationBaseUrl}}/api/registrations/{{registrationId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Example response:

```json
{
  "id": 100,
  "eventId": 10,
  "participantId": 2,
  "status": "CANCELLED",
  "registeredAt": "2026-04-24T15:30:00",
  "cancelledAt": "2026-04-24T15:40:00"
}
```

### Request 12: Confirm Count After Cancellation

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/events/{{eventId}}/count`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- `registeredCount` decreases after cancellation

### Request 13: Reject Missing Token

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/me`

No Authorization header.

Expected:

- `401 Unauthorized`

### Request 14: Reject Invalid Token

- Method: `GET`
- URL: `{{registrationBaseUrl}}/api/registrations/me`

Headers:

- `Authorization: Bearer abc.def.ghi`

Expected:

- `401 Unauthorized`

## 13. Recommended Postman Order

1. Login participant
2. Login admin
3. Confirm event exists
4. Confirm event availability
5. Create registration
6. Reject duplicate registration
7. Get my registrations
8. Get registration by id
9. Get event count
10. Verify participant tracking access control
11. Cancel registration
12. Confirm count after cancellation
13. Test missing token
14. Test invalid token

## 14. Contract Confirmation Checklist

Before you say your part is integrated, confirm:

- `event-service` returns `id`, `title`, and `status` from `GET /api/events/{eventId}`
- `event-service` returns `eventId`, `status`, `availableSeats`, and `registrationOpen` from `GET /api/events/{eventId}/availability`
- `notification-service` accepts (Phase 5 integration check only):

```json
{
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

- `notification-service` also accepts (Phase 5 integration check only):

```json
{
  "userId": 1,
  "type": "REGISTRATION_CANCELLED",
  "title": "Registration Cancelled",
  "message": "Your registration was cancelled for Spring Boot Workshop"
}
```

If these contracts differ, integration can fail even if your local tests pass.

## 15. If Something Fails

First, look at registration-service logs:

```powershell
docker compose logs -f registration-service
```

Then check:

- did you use `{{registrationBaseUrl}} = http://localhost:8083`
- did you send `Authorization: Bearer {{participantToken}}`
- does the event really exist
- is the event `SCHEDULED`
- does the availability endpoint return seats
- is `event-service` running
- `notification-service` is optional for Person 3 testing
- did you use the latest token after login

## 16. How Person 3 Knows The Registration Part Is Done

Person 3 can mark the registration task done when all of these are true:

- registration-service starts successfully
- `mvn -pl registration-service test` passes
- registration creation works
- duplicate registration is rejected
- `/api/registrations/me` works
- `/api/registrations/{registrationId}` works for the owner
- cancellation works
- `/api/registrations/events/{eventId}/count` works
- `/api/registrations/events/{eventId}` returns `403` for participant and `200` for admin
- missing token is rejected
- invalid token is rejected
- event-service contract is confirmed
- notification-service contract check is deferred to Phase 5 integration

## 17. Fast Final Checklist

Run tests:

```powershell
mvn -pl registration-service test
```

Run registration stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service
```

Then complete the Postman flow above.

If all expected results match, Person 3 is ready to report the registration-service task as complete.
