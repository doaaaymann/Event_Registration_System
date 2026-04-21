# Event Service Work And Testing Guide

This guide is for Person 2, the owner of `event-service`.

It explains:

- how to open the project
- how to run only the services needed for event testing
- how to work inside your part safely
- how to run the event tests
- how to test the event APIs in Postman
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

## 2. What Person 2 Owns

Person 2 owns only the event microservice.

You should mainly work inside:

- `event-service/src/main/java`
- `event-service/src/main/resources`
- `event-service/src/test/java`
- `event-service/pom.xml`

Avoid editing:

- `auth-service`
- `registration-service`
- `notification-service`
- `api-gateway`
- `config-server`
- `eureka-server`
- `docker-compose.yml`

unless the team agrees first.

## 3. What Person 2 Is Responsible For

According to the task plan, Person 2 is responsible for:

- event creation
- event update
- event cancellation
- event rescheduling
- event listing
- seat limits
- organizer permissions
- event database tables
- event tests

Person 2 also delivers these contracts to others:

- event details
- event availability
- event state changes

## 4. Run The Services Needed For Event Testing

If you want the smallest useful stack for event testing, run:

```powershell
docker compose up --build postgres config-server eureka-server auth-service registration-service event-service
```

This starts:

- PostgreSQL
- Config Server
- Eureka Server
- Auth Service
- Registration Service
- Event Service

To stop the containers:

```powershell
docker compose down
```

To watch only event-service logs:

```powershell
docker compose logs -f event-service
```

To watch registration-service logs too:

```powershell
docker compose logs -f registration-service
```

## 5. Useful URLs

Direct event-service base URL:

```text
http://localhost:8082
```

Auth-service base URL:

```text
http://localhost:8081
```

If later you want to test through the API Gateway:

```text
http://localhost:8080
```

For Person 2 testing, use the direct event-service URL first:

```text
http://localhost:8082
```

## 6. How To Work On Your Part

Before changing code:

1. go to the root project folder
2. make sure the event stack can run
3. work only inside `event-service`
4. after changes, run event tests
5. then test manually in Postman

Recommended local workflow:

```powershell
cd C:\Users\doaaa\Documents\GitHub\Event_Registration_System-
```

```powershell
mvn -pl event-service,registration-service test
```

If tests pass, run the event stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service registration-service event-service
```

Then verify the endpoints in Postman.

## 7. How To Run The Event Tests

Run all event-service and registration count integration tests:

```powershell
mvn -pl event-service,registration-service test
```

Run only event-service tests:

```powershell
mvn -pl event-service test
```

Run only registration-service tests:

```powershell
mvn -pl registration-service test
```

Run only event service tests:

```powershell
mvn -pl event-service -Dtest=EventServiceTest test
```

Run only event controller tests:

```powershell
mvn -pl event-service -Dtest=EventControllerTest test
```

Expected result:

- build success
- all event-service tests pass
- registration count endpoint tests pass

## 8. Auth Accounts To Use

The auth service seeds a default admin account.

Use:

- email: `admin@event.local`
- password: `Admin12345`

Important note:

- public organizer registration is rejected by `auth-service`
- the current easiest manual test actor for event management is the seeded admin account
- event-service still enforces organizer or admin permissions

If the team later adds organizer creation by admin, you can repeat the same tests with a real organizer token.

## 9. Postman Setup

Create a Postman environment and add these variables:

- `eventBaseUrl = http://localhost:8082`
- `authBaseUrl = http://localhost:8081`
- `adminToken =`
- `participantToken =`
- `eventId =`
- `adminUserId = 1`

Optional if you also test through the gateway later:

- `gatewayBaseUrl = http://localhost:8080`

## 10. Important Postman Rules

- Always use `{{eventBaseUrl}}` for direct event testing first
- Use `{{authBaseUrl}}` only for login
- For protected event endpoints, send `Authorization: Bearer <token>`
- Use `/events/10`, not `/events/{10}`
- Test event-service directly before testing through the gateway
- The current count integration depends on `registration-service` being up

## 11. Postman Test Flow

### Request 1: Login Admin

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

Example response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@event.local",
  "roles": ["ADMIN"]
}
```

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("adminToken", json.accessToken);
pm.environment.set("adminUserId", json.userId);
```

### Request 2: Create Event

- Method: `POST`
- URL: `{{eventBaseUrl}}/api/events`

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer {{adminToken}}`

Body:

```json
{
  "title": "Spring Boot Workshop",
  "description": "Hands-on workshop",
  "location": "Cairo Hall A",
  "startTime": "2026-05-01T10:00:00",
  "endTime": "2026-05-01T13:00:00",
  "maxSeats": 100,
  "organizerId": 1
}
```

Expected:

- `201 Created`

Example response:

```json
{
  "id": 10,
  "title": "Spring Boot Workshop",
  "status": "SCHEDULED",
  "availableSeats": 100
}
```

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("eventId", json.id);
```

### Request 3: List Events

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`
- response contains the created event

Expected response shape:

```json
[
  {
    "id": 10,
    "title": "Spring Boot Workshop",
    "description": "Hands-on workshop",
    "location": "Cairo Hall A",
    "startTime": "2026-05-01T10:00:00",
    "endTime": "2026-05-01T13:00:00",
    "maxSeats": 100,
    "registeredCount": 0,
    "availableSeats": 100,
    "status": "SCHEDULED",
    "organizerId": 1
  }
]
```

### Request 4: Get Event Details

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`

Expected response shape:

```json
{
  "id": 10,
  "title": "Spring Boot Workshop",
  "description": "Hands-on workshop",
  "location": "Cairo Hall A",
  "startTime": "2026-05-01T10:00:00",
  "endTime": "2026-05-01T13:00:00",
  "maxSeats": 100,
  "registeredCount": 0,
  "availableSeats": 100,
  "status": "SCHEDULED",
  "organizerId": 1
}
```

### Request 5: Get Organizer Events

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events/organizers/{{adminUserId}}`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`
- response contains the created event

### Request 6: Get Event Availability

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`

Expected response shape:

```json
{
  "eventId": 10,
  "status": "SCHEDULED",
  "maxSeats": 100,
  "registeredCount": 0,
  "availableSeats": 100,
  "registrationOpen": true
}
```

### Request 7: Update Event

- Method: `PUT`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}`

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer {{adminToken}}`

Body:

```json
{
  "title": "Spring Boot Workshop Updated",
  "description": "Hands-on workshop with labs",
  "location": "Cairo Hall B",
  "startTime": "2026-05-01T11:00:00",
  "endTime": "2026-05-01T14:00:00",
  "maxSeats": 120
}
```

Expected:

- `200 OK`

Then verify:

- title changed
- location changed
- maxSeats changed to `120`
- status remains `SCHEDULED`

### Request 8: Reschedule Event

- Method: `PATCH`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/reschedule`

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer {{adminToken}}`

Body:

```json
{
  "startTime": "2026-05-02T10:00:00",
  "endTime": "2026-05-02T13:00:00"
}
```

Expected:

- `200 OK`
- status becomes `RESCHEDULED`

### Request 9: Cancel Event

- Method: `PATCH`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/cancel`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`
- status becomes `CANCELLED`

### Request 10: Availability After Cancellation

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `200 OK`
- `registrationOpen` should be `false`

### Request 11: Reject Update After Cancellation

- Method: `PUT`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}`

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer {{adminToken}}`

Body:

```json
{
  "title": "Should Fail",
  "description": "This should not succeed",
  "location": "Hall X",
  "startTime": "2026-05-03T10:00:00",
  "endTime": "2026-05-03T13:00:00",
  "maxSeats": 50
}
```

Expected:

- `400 Bad Request`

### Request 12: Reject Double Cancellation

- Method: `PATCH`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/cancel`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `400 Bad Request`

### Request 13: Reject Missing Token

- Method: `POST`
- URL: `{{eventBaseUrl}}/api/events`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "title": "Unauthorized Event",
  "description": "No token",
  "location": "Hall A",
  "startTime": "2026-05-10T10:00:00",
  "endTime": "2026-05-10T12:00:00",
  "maxSeats": 20,
  "organizerId": 1
}
```

Expected:

- `401 Unauthorized`

### Request 14: Reject Invalid Token

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events`

Headers:

- `Authorization: Bearer abc.def.ghi`

Expected:

- `401 Unauthorized`

### Request 15: Optional Count Integration Check

Use this request only if Person 3 has created real registrations for the event.

- Method: `GET`
- URL: `{{eventBaseUrl}}/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{adminToken}}`

Expected:

- `registeredCount` should match `registration-service`
- `availableSeats` should equal `maxSeats - registeredCount`

## 12. Recommended Postman Order

1. Login admin
2. Create event
3. List events
4. Get event details
5. Get organizer events
6. Get availability
7. Update event
8. Reschedule event
9. Cancel event
10. Re-check availability
11. Try forbidden update after cancellation
12. Try double cancellation
13. Test missing token
14. Test invalid token
15. If registration exists, verify real count integration

## 13. If Something Fails

First, look at event-service logs:

```powershell
docker compose logs -f event-service
```

If the count is wrong or always zero, also check:

```powershell
docker compose logs -f registration-service
```

Then check:

- did you use `{{eventBaseUrl}} = http://localhost:8082`
- did you use `{{authBaseUrl}} = http://localhost:8081`
- did you send `Authorization: Bearer <token>`
- did you use a valid token from admin login
- is `registration-service` running
- did you rebuild after code changes
- did you use the latest `eventId`

## 14. How Person 2 Knows The Event Part Is Done

Person 2 can mark the event task done when all of these are true:

- event-service starts successfully
- `mvn -pl event-service,registration-service test` passes
- create event works
- list events works
- get event details works
- organizer event listing works
- update works
- reschedule works
- cancel works
- cancelled events cannot be modified
- seat availability response is correct
- event-service can read registration counts from `registration-service`
- unauthorized requests are rejected

## 15. Fast Final Checklist

Run tests:

```powershell
mvn -pl event-service,registration-service test
```

Run the event stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service registration-service event-service
```

Then complete the Postman flow above.

If all expected results match, Person 2 is ready to report the event-service task as complete.
