# Notification Service Work And Testing Guide

This guide is for Person 4, the owner of `notification-service`.

It explains:

- how to open the project
- how to run only the services needed for notification testing
- how to work inside your part safely
- how to test the notification APIs in Postman
- how to decide that your part is finished

## 1. Open The Project

Open a terminal in the project root and move into the repository:

```powershell
cd C:\Users\asus\OneDrive\Documents\GitHub\Event_Registration_System
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

## 2. What Person 4 Owns

Person 4 owns only the notification microservice.

You should mainly work inside:

- `notification-service/src/main/java`
- `notification-service/src/main/resources`
- `notification-service/src/test/java`
- `notification-service/pom.xml`

Avoid editing:

- `auth-service`
- `event-service`
- `registration-service`
- `api-gateway`
- `config-server`
- `eureka-server`
- `docker-compose.yml`

unless the team agrees first.

## 3. What Person 4 Is Responsible For

According to the task plan, Person 4 is responsible for:

- notification creation
- notification retrieval
- read status updates
- event and registration notification triggers
- notification database tables
- notification tests

## 4. Run Only The Notification Stack With Docker

If you only want the services needed for notification testing, run:

```powershell
docker compose up --build postgres config-server eureka-server auth-service registration-service notification-service
```

This starts:

- PostgreSQL
- Config Server
- Eureka Server
- Auth Service
- Registration Service
- Notification Service

If you want to test event cancellation and reschedule notification triggers too, include `event-service`:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service notification-service
```

To stop the containers:

```powershell
docker compose down
```

To watch only notification-service logs:

```powershell
docker compose logs -f notification-service
```

## 5. Useful URLs

Direct notification-service base URL:

```text
http://localhost:8084
```

If later you want to test through the API Gateway:

```text
http://localhost:8080
```

For Person 4 testing, use the direct notification-service URL first:

```text
http://localhost:8084
```

## 6. How To Work On Your Part

Before changing code:

1. go to the root project folder
2. make sure the notification stack can run
3. work only inside `notification-service`
4. run notification tests locally
5. then test manually in Postman

Recommended local workflow:

```powershell
cd C:\Users\asus\OneDrive\Documents\GitHub\Event_Registration_System
```

```powershell
mvn -pl notification-service test
```

If tests pass, run the notification stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service notification-service
```

Then verify the endpoints in Postman.

## 7. How To Run The Notification Tests

Run all notification-service tests:

```powershell
mvn -pl notification-service test
```

Run only service tests:

```powershell
mvn -pl notification-service -Dtest=NotificationServiceTest test
```

Run only controller tests:

```powershell
mvn -pl notification-service -Dtest=NotificationControllerTest test
```

Run only JWT filter test:

```powershell
mvn -pl notification-service -Dtest=JwtAuthenticationFilterTest test
```

Expected result:

- build success
- all notification tests pass

## 8. Required Service Contracts

Person 4 depends on these external payload agreements:

From `registration-service`:

- `POST /api/notifications/internal/registration-created`
- `POST /api/notifications/internal/registration-cancelled`

Current compatible request body:

```json
{
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

From `event-service`:

- `POST /api/notifications/internal/event-cancelled`
- `POST /api/notifications/internal/event-rescheduled`
- `event-service` is now wired to call these notification endpoints automatically from:
  - `PATCH /api/events/{eventId}/cancel`
  - `PATCH /api/events/{eventId}/reschedule`

Supported request bodies for event triggers:

Single recipient:

```json
{
  "userId": 1,
  "type": "EVENT_CANCELLED",
  "title": "Event Cancelled",
  "message": "Spring Boot Workshop was cancelled"
}
```

Multiple recipients:

```json
{
  "userIds": [1, 2, 3],
  "type": "EVENT_RESCHEDULED",
  "title": "Event Rescheduled",
  "message": "Spring Boot Workshop has been rescheduled"
}
```

## 9. Postman Setup

Create a Postman environment and add these variables:

- `notificationBaseUrl = http://localhost:8084`
- `eventBaseUrl = http://localhost:8082`
- `registrationBaseUrl = http://localhost:8083`
- `authBaseUrl = http://localhost:8081`
- `participantEmail = testnotify@example.com`
- `participantPassword = Secret123`
- `participantToken =`
- `participantUserId =`
- `adminToken =`
- `notificationId =`

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

- Always use `{{notificationBaseUrl}}` for notification requests
- Use `{{authBaseUrl}}` only for login
- For protected endpoints, send `Authorization: Bearer {{participantToken}}` or `Authorization: Bearer {{adminToken}}`
- Use `/notifications/501/read`, not `/notifications/{501}/read`
- Test `notification-service` directly before testing through the gateway

## 12. Postman Test Flow

### Request 1: Register Participant

- Method: `POST`
- URL: `{{authBaseUrl}}/api/auth/register`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "fullName": "Notification Flow User",
  "email": "{{participantEmail}}",
  "password": "{{participantPassword}}",
  "role": "PARTICIPANT"
}
```

Expected:

- `201 Created` on the first run
- `400 Bad Request` with `Email is already registered` on later runs

If the email already exists, continue with login using the same credentials.

### Request 2: Login Participant

- Method: `POST`
- URL: `{{authBaseUrl}}/api/auth/login`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "email": "{{participantEmail}}",
  "password": "{{participantPassword}}"
}
```

Expected:

- `200 OK`

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("participantToken", json.accessToken);
pm.environment.set("participantUserId", json.userId);
```

### Request 3: Login Admin

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

### Request 4: Create Notification As Admin

- Method: `POST`
- URL: `{{notificationBaseUrl}}/api/notifications`

Headers:

- `Authorization: Bearer {{adminToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "userId": {{participantUserId}},
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

Expected:

- `201 Created`

Example response:

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

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("notificationId", json.id);
```

### Request 5: Get My Notifications As Participant

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/{{participantUserId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- array contains the created notification

### Request 6: Reject Reading Another User Notifications

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/2`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `403 Forbidden`

### Request 7: Mark Notification As Read

- Method: `PATCH`
- URL: `{{notificationBaseUrl}}/api/notifications/{{notificationId}}/read`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Example response:

```json
{
  "id": 501,
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop",
  "read": true,
  "createdAt": "2026-04-26T16:20:00"
}
```

### Request 8: Trigger Internal Registration Notification

- Method: `POST`
- URL: `{{notificationBaseUrl}}/api/notifications/internal/registration-created`

Headers:

- `Authorization: Bearer {{participantToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "userId": {{participantUserId}},
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

Expected:

- `201 Created`
- response is an array with one created notification

### Request 9: Trigger Internal Cancellation Notification

- Method: `POST`
- URL: `{{notificationBaseUrl}}/api/notifications/internal/registration-cancelled`

Headers:

- `Authorization: Bearer {{participantToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "userId": {{participantUserId}},
  "type": "REGISTRATION_CANCELLED",
  "title": "Registration Cancelled",
  "message": "Your registration was cancelled for Spring Boot Workshop"
}
```

Expected:

- `201 Created`
- response is an array with one created notification

### Request 10: Trigger Event Cancellation Notification For Many Users

- Method: `POST`
- URL: `{{notificationBaseUrl}}/api/notifications/internal/event-cancelled`

Headers:

- `Authorization: Bearer {{adminToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "userIds": [1, 2, 3],
  "type": "EVENT_CANCELLED",
  "title": "Event Cancelled",
  "message": "Spring Boot Workshop was cancelled"
}
```

Expected:

- `201 Created`
- response array length is `3`

### Request 11: Trigger Event Rescheduled Notification For Many Users

- Method: `POST`
- URL: `{{notificationBaseUrl}}/api/notifications/internal/event-rescheduled`

Headers:

- `Authorization: Bearer {{adminToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "userIds": [1, 2, 3],
  "type": "EVENT_RESCHEDULED",
  "title": "Event Rescheduled",
  "message": "Spring Boot Workshop has been rescheduled"
}
```

Expected:

- `201 Created`
- response array length is `3`

### Request 12: Registration-Service Integration Test

This verifies the real microservice flow:

- `event-service` -> `registration-service` -> `notification-service`

Start the full stack:

```powershell
docker compose up --build -d postgres config-server eureka-server auth-service event-service registration-service notification-service
```

Important startup note:

- `event-service` and `registration-service` can fail on the first start while `config-server` is still coming up.
- if either service is missing from `docker compose ps`, restart them:

```powershell
docker compose restart event-service registration-service
```

Test order:

1. register a fresh participant
2. login as participant
3. login as admin
4. create an event through `event-service`
5. create a registration through `registration-service`
6. fetch participant notifications through `notification-service`
7. cancel the registration through `registration-service`
8. fetch participant notifications again

Create event request:

- Method: `POST`
- URL: `{{eventBaseUrl}}/api/events`

Headers:

- `Authorization: Bearer {{adminToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "title": "Registration Notification Test",
  "description": "Verify registration to notification flow",
  "location": "Hall C",
  "startTime": "2026-05-10T10:00:00",
  "endTime": "2026-05-10T12:00:00",
  "maxSeats": 25,
  "organizerId": 1
}
```

Expected:

- `201 Created`

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("eventId", json.id);
```

Create registration request:

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
- response contains `"status": "REGISTERED"`

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("registrationId", json.id);
```

Fetch notifications after registration:

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/{{participantUserId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected latest notification:

```json
{
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Registration Notification Test"
}
```

Cancel registration request:

- Method: `DELETE`
- URL: `{{registrationBaseUrl}}/api/registrations/{{registrationId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- response contains `"status": "CANCELLED"`

Fetch notifications after cancellation:

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/{{participantUserId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected latest notification:

```json
{
  "type": "REGISTRATION_CANCELLED",
  "title": "Registration Cancelled",
  "message": "Your registration was cancelled for Registration Notification Test"
}
```

If you want to run the same flow from PowerShell, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\tasks\REGISTRATION_NOTIFICATION_POWERSHELL_TEST.ps1
```

### Request 13: Reject Missing Token

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/1`

No Authorization header.

Expected:

- `401 Unauthorized`

### Request 14: Reject Invalid Token

- Method: `GET`
- URL: `{{notificationBaseUrl}}/api/notifications/users/1`

Headers:

- `Authorization: Bearer abc.def.ghi`

Expected:

- `401 Unauthorized`

## 13. Recommended Postman Order

1. Login participant
2. Login admin
3. Create notification as admin
4. Get my notifications as participant
5. Reject reading another user notifications
6. Mark notification as read
7. Trigger internal registration notification
8. Trigger internal cancellation notification
9. Trigger event cancellation notification
10. Trigger event rescheduled notification
11. Run the full event-service integration test
12. Test missing token
13. Test invalid token

## 14. Contract Confirmation Checklist

Before you say your part is integrated, confirm:

- `POST /api/notifications` accepts:

```json
{
  "userId": 1,
  "type": "REGISTRATION_CONFIRMED",
  "title": "Registration Confirmed",
  "message": "You are registered for Spring Boot Workshop"
}
```

- `GET /api/notifications/users/{userId}` returns an array of notification objects
- `PATCH /api/notifications/{notificationId}/read` sets `read = true`
- `registration-service` can call:
  - `POST /api/notifications/internal/registration-created`
  - `POST /api/notifications/internal/registration-cancelled`
- `event-service` can call:
  - `POST /api/notifications/internal/event-cancelled`
  - `POST /api/notifications/internal/event-rescheduled`
- event trigger endpoints accept either `userId` or `userIds`
- event-service reschedule should create `EVENT_RESCHEDULED` notifications for registered participants
- event-service cancel should create `EVENT_CANCELLED` notifications for registered participants

If these contracts differ, integration can fail even if local tests pass.

## 15. If Something Fails

First, look at notification-service logs:

```powershell
docker compose logs -f notification-service
```

Then check:

- did you use `{{notificationBaseUrl}} = http://localhost:8084`
- did you send `Authorization: Bearer {{participantToken}}` or `{{adminToken}}`
- does the token contain the expected role
- is PostgreSQL running
- did Flyway create the `notifications` table
- did you use `userId` or `userIds` correctly for internal trigger calls
- did you use the latest token after login

## 16. How Person 4 Knows The Notification Part Is Done

Person 4 can mark the notification task done when all of these are true:

- notification-service starts successfully
- `mvn -pl notification-service test` passes
- manual notification creation works
- `/api/notifications/users/{userId}` works for the owner
- reading another user's notifications returns `403`
- marking a notification as read works
- internal registration trigger works
- internal cancellation trigger works
- internal event cancellation trigger works
- internal event reschedule trigger works
- event-service end-to-end cancel/reschedule integration works
- missing token is rejected
- invalid token is rejected
- registration-service contract is confirmed
- event-service trigger payload is confirmed

## 17. Fast Final Checklist

Run tests:

```powershell
mvn -pl notification-service test
```

Run notification stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service event-service registration-service notification-service
```

Then complete the Postman flow above.

If all expected results match, Person 4 is ready to report the notification-service task as complete.
