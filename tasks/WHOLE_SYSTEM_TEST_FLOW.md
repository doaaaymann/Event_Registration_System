# Whole System Test Flow

This guide shows how to test the full backend flow of the Event Registration System through the API Gateway.

It covers:

- Docker startup
- login and role setup
- organizer creation
- participant registration
- event creation
- event registration
- participant tracking
- reschedule and cancel
- notification verification

All requests below go through:

```text
http://localhost:8080
```

## 1. Start The Full System

From the project root:

```powershell
cd C:\Users\doaaa\Documents\GitHub\Event_Registration_System-
docker compose up --build -d
docker compose ps
```

Expected:

- `postgres` is `healthy`
- `config-server`, `eureka-server`, `api-gateway`, `auth-service`, `event-service`, `registration-service`, and `notification-service` are `Up`

Optional health checks:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8084/actuator/health
```

## 2. Important Admin Password Note

There are two possible admin passwords depending on your Docker volume state:

- fresh database expected default: `EventAdmin123!`
- reused older database volume may still use: `Admin12345`

Use this order:

1. try `EventAdmin123!`
2. if login returns `401`, try `Admin12345`

If you want the new default for sure, remove old containers and volumes first:

```powershell
docker compose down -v
docker compose up --build -d
```

## 3. Postman Environment

Create a Postman environment with these variables:

- `baseUrl = http://localhost:8080`
- `adminEmail = admin@event.local`
- `adminPassword = EventAdmin123!`
- `adminPasswordFallback = Admin12345`
- `adminToken =`
- `organizerEmail = organizer@example.com`
- `organizerPassword = Secret123`
- `organizerToken =`
- `organizerId =`
- `participantEmail = participant@example.com`
- `participantPassword = Secret123`
- `participantToken =`
- `participantId =`
- `eventId =`
- `registrationId =`

Tip:

- if you rerun the flow many times, change organizer and participant emails each time
- easiest pattern is `organizer+001@example.com`, `participant+001@example.com`

## 4. Full Postman Flow

### Request 1: Login Admin

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/login`

Body:

```json
{
  "email": "{{adminEmail}}",
  "password": "{{adminPassword}}"
}
```

Expected:

- `200 OK`

Save token:

```javascript
const json = pm.response.json();
pm.environment.set("adminToken", json.accessToken);
```

If this fails with `401`, retry once with:

```json
{
  "email": "{{adminEmail}}",
  "password": "{{adminPasswordFallback}}"
}
```

If fallback works, update `adminPassword` in your environment to `Admin12345`.

### Request 2: Create Organizer As Admin

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/admin/users`

Headers:

- `Authorization: Bearer {{adminToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "fullName": "Omar Organizer",
  "email": "{{organizerEmail}}",
  "password": "{{organizerPassword}}",
  "role": "ORGANIZER"
}
```

Expected:

- `201 Created`

Save organizer id:

```javascript
const json = pm.response.json();
pm.environment.set("organizerId", json.id);
```

### Request 3: Login Organizer

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/login`

Body:

```json
{
  "email": "{{organizerEmail}}",
  "password": "{{organizerPassword}}"
}
```

Expected:

- `200 OK`
- role contains `ORGANIZER`

Save token:

```javascript
const json = pm.response.json();
pm.environment.set("organizerToken", json.accessToken);
```

### Request 4: Register Participant

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/register`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "fullName": "Participant One",
  "email": "{{participantEmail}}",
  "password": "{{participantPassword}}",
  "role": "PARTICIPANT"
}
```

Expected:

- `201 Created`

Save participant id:

```javascript
const json = pm.response.json();
pm.environment.set("participantId", json.id);
```

### Request 5: Login Participant

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/login`

Body:

```json
{
  "email": "{{participantEmail}}",
  "password": "{{participantPassword}}"
}
```

Expected:

- `200 OK`

Save token:

```javascript
const json = pm.response.json();
pm.environment.set("participantToken", json.accessToken);
```

### Request 6: Create Event As Organizer

- Method: `POST`
- URL: `{{baseUrl}}/api/events`

Headers:

- `Authorization: Bearer {{organizerToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "title": "Gateway Verified Workshop",
  "description": "End-to-end gateway verification",
  "location": "Hall A",
  "startTime": "2026-06-15T10:00:00",
  "endTime": "2026-06-15T13:00:00",
  "maxSeats": 5,
  "organizerId": {{organizerId}}
}
```

Expected:

- `201 Created`

Save event id:

```javascript
const json = pm.response.json();
pm.environment.set("eventId", json.id);
```

### Request 7: Check Event Availability

- Method: `GET`
- URL: `{{baseUrl}}/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- `registeredCount = 0`
- `availableSeats = 5`
- `registrationOpen = true`

### Request 8: Register Participant To Event

- Method: `POST`
- URL: `{{baseUrl}}/api/registrations`

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
- status is `REGISTERED`

Save registration id:

```javascript
const json = pm.response.json();
pm.environment.set("registrationId", json.id);
```

### Request 9: Organizer Tracks Participants

- Method: `GET`
- URL: `{{baseUrl}}/api/registrations/events/{{eventId}}`

Headers:

- `Authorization: Bearer {{organizerToken}}`

Expected:

- `200 OK`
- array contains the participant registration

### Request 10: Verify Registration Notification

- Method: `GET`
- URL: `{{baseUrl}}/api/notifications/users/{{participantId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- latest or returned list contains `REGISTRATION_CONFIRMED`

### Request 11: Reschedule Event

- Method: `PATCH`
- URL: `{{baseUrl}}/api/events/{{eventId}}/reschedule`

Headers:

- `Authorization: Bearer {{organizerToken}}`
- `Content-Type: application/json`

Body:

```json
{
  "startTime": "2026-06-16T10:00:00",
  "endTime": "2026-06-16T13:00:00"
}
```

Expected:

- `200 OK`
- status becomes `RESCHEDULED`

### Request 12: Verify Reschedule Notification

- Method: `GET`
- URL: `{{baseUrl}}/api/notifications/users/{{participantId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- list contains `EVENT_RESCHEDULED`

### Request 13: Cancel Event

- Method: `PATCH`
- URL: `{{baseUrl}}/api/events/{{eventId}}/cancel`

Headers:

- `Authorization: Bearer {{organizerToken}}`

Expected:

- `200 OK`
- status becomes `CANCELLED`

### Request 14: Verify Cancel Notification

- Method: `GET`
- URL: `{{baseUrl}}/api/notifications/users/{{participantId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- list contains `EVENT_CANCELLED`

### Request 15: Verify Registration Closed After Cancel

- Method: `GET`
- URL: `{{baseUrl}}/api/events/{{eventId}}/availability`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`
- `registrationOpen = false`

## 5. Expected Successful Outcome

If the full project flow is healthy, you should have:

- admin login working
- admin-created organizer working
- organizer login working
- participant registration/login working
- event creation working
- participant event registration working
- participant tracking working
- reschedule working
- cancel working
- notifications visible for:
  - `REGISTRATION_CONFIRMED`
  - `EVENT_RESCHEDULED`
  - `EVENT_CANCELLED`

## 6. Good Negative Tests

After the main happy path, test these too:

### Participant Cannot Create Event

- `POST {{baseUrl}}/api/events`
- use `participantToken`
- expect `403 Forbidden`

### Public Organizer Registration Is Rejected

- `POST {{baseUrl}}/api/auth/register`
- role = `ORGANIZER`
- expect `400 Bad Request`

### Duplicate Registration Is Rejected

- repeat event registration as the same participant
- expect `409 Conflict`

### Missing Token Is Rejected

- call a protected endpoint without `Authorization`
- expect `401 Unauthorized`

## 7. Troubleshooting

### `401` On Admin Login

Cause:

- reused Docker volume still has old seeded admin password

Fix:

- try `Admin12345`
- or reset with `docker compose down -v`

### `503` On Notification Routes

Cause:

- `notification-service` is not healthy or not registered in Eureka yet

Fix:

```powershell
docker compose ps
docker compose logs notification-service --tail 100
Invoke-RestMethod http://localhost:8084/actuator/health
```

### Service Starts But Gateway Flow Fails

Check:

```powershell
docker compose logs api-gateway --tail 100
docker compose logs auth-service --tail 100
docker compose logs event-service --tail 100
docker compose logs registration-service --tail 100
docker compose logs notification-service --tail 100
```

### Fresh Start

If you want a fully clean environment:

```powershell
docker compose down -v
docker compose up --build -d
```

## 8. Final Result

If all steps above pass, the whole backend project flow is working correctly through the Dockerized microservice setup and API Gateway.
