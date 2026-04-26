# Docker Testing Guide

## Scope

This guide shows how to test:

- each microservice individually
- the full system through the gateway
- the fail-closed behavior

All commands below are written for Windows PowerShell.

## Start the stack

```powershell
docker compose up -d
docker compose ps
```

Expected result:

- every container is `Up`
- `postgres` is `healthy`

## 1. Test every service health endpoint

```powershell
Invoke-RestMethod http://localhost:8888/actuator/health
Invoke-RestMethod http://localhost:8761/actuator/health
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8083/actuator/health
Invoke-RestMethod http://localhost:8084/actuator/health
```

Expected output pattern:

```json
{
  "status": "UP"
}
```

## 2. Test config-server directly

```powershell
Invoke-RestMethod http://localhost:8888/auth-service/default
```

Expected output:

- JSON containing:
  - `"name": "auth-service"`
  - `"profiles": ["default"]`
  - property sources for the config repo

## 3. Test eureka-server directly

Open:

- [http://localhost:8761](http://localhost:8761)

Expected result:

- Eureka dashboard loads
- registered services appear after startup

## 4. Test auth-service directly

### 4.1 Register a participant

```powershell
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$participantEmail = "participant+$stamp@example.com"

$participant = Invoke-RestMethod `
  -Uri http://localhost:8081/api/auth/register `
  -Method Post `
  -ContentType 'application/json' `
  -Body (@{
    fullName = 'Sara Ali'
    email = $participantEmail
    password = 'Secret123!'
    role = 'PARTICIPANT'
  } | ConvertTo-Json)

$participant
```

Expected output pattern:

```json
{
  "id": 12,
  "fullName": "Sara Ali",
  "email": "participant+20260426210000@example.com",
  "status": "ACTIVE",
  "roles": [
    "PARTICIPANT"
  ]
}
```

### 4.2 Login as participant

```powershell
$participantLogin = Invoke-RestMethod `
  -Uri http://localhost:8081/api/auth/login `
  -Method Post `
  -ContentType 'application/json' `
  -Body (@{
    email = $participantEmail
    password = 'Secret123!'
  } | ConvertTo-Json)

$participantLogin
```

Expected output pattern:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "userId": 12,
  "email": "participant+...@example.com",
  "roles": [
    "PARTICIPANT"
  ]
}
```

### 4.3 Login as admin

Fresh database expected demo credentials:

- email: `admin@event.local`
- password: `ChangeMeNow!123`

If you reused an old PostgreSQL Docker volume, the old seeded password may still be:

- `Admin12345`

```powershell
$adminLogin = Invoke-RestMethod `
  -Uri http://localhost:8081/api/auth/login `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"email":"admin@event.local","password":"ChangeMeNow!123"}'
```

If that fails on an old reused volume:

```powershell
$adminLogin = Invoke-RestMethod `
  -Uri http://localhost:8081/api/auth/login `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"email":"admin@event.local","password":"Admin12345"}'
```

Expected output pattern:

```json
{
  "tokenType": "Bearer",
  "userId": 1,
  "roles": [
    "ADMIN"
  ]
}
```

## 5. Test event-service directly

```powershell
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }

$event = Invoke-RestMethod `
  -Uri http://localhost:8082/api/events `
  -Method Post `
  -Headers $adminHeaders `
  -ContentType 'application/json' `
  -Body (@{
    title = 'Spring Microservices Workshop'
    description = 'Hands-on session'
    location = 'Hall A'
    startTime = '2026-05-10T10:00:00'
    endTime = '2026-05-10T13:00:00'
    maxSeats = 2
    organizerId = 1
  } | ConvertTo-Json)

$event
```

Expected output pattern:

```json
{
  "id": 7,
  "title": "Spring Microservices Workshop",
  "status": "SCHEDULED",
  "availableSeats": 2
}
```

Check availability:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8082/api/events/$($event.id)/availability" `
  -Method Get `
  -Headers $adminHeaders
```

Expected output pattern:

```json
{
  "eventId": 7,
  "status": "SCHEDULED",
  "maxSeats": 2,
  "registeredCount": 0,
  "availableSeats": 2,
  "registrationOpen": true
}
```

## 6. Test registration-service directly

```powershell
$participantHeaders = @{ Authorization = "Bearer $($participantLogin.accessToken)" }

$registration1 = Invoke-RestMethod `
  -Uri http://localhost:8083/api/registrations `
  -Method Post `
  -Headers $participantHeaders `
  -ContentType 'application/json' `
  -Body (@{ eventId = $event.id } | ConvertTo-Json)

$registration1
```

Expected output pattern:

```json
{
  "id": 20,
  "eventId": 7,
  "participantId": 12,
  "status": "REGISTERED",
  "registeredAt": "2026-04-26T21:00:00"
}
```

Cancel it:

```powershell
$cancelled = Invoke-RestMethod `
  -Uri "http://localhost:8083/api/registrations/$($registration1.id)" `
  -Method Delete `
  -Headers $participantHeaders

$cancelled
```

Expected output:

```json
{
  "status": "CANCELLED"
}
```

Re-register:

```powershell
$registration2 = Invoke-RestMethod `
  -Uri http://localhost:8083/api/registrations `
  -Method Post `
  -Headers $participantHeaders `
  -ContentType 'application/json' `
  -Body (@{ eventId = $event.id } | ConvertTo-Json)

$registration2
```

Expected output:

```json
{
  "status": "REGISTERED"
}
```

Check active count:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8083/api/registrations/events/$($event.id)/count" `
  -Method Get `
  -Headers $participantHeaders
```

Expected output pattern:

```json
{
  "eventId": 7,
  "registeredCount": 1
}
```

## 7. Test notification-service directly

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8084/api/notifications/users/$($participant.id)" `
  -Method Get `
  -Headers $participantHeaders
```

Expected output:

- JSON array
- after register, cancel, and re-register, the participant should usually have at least:
  - one registration confirmed notification
  - one registration cancelled notification
  - one second registration confirmed notification

Expected array count after that flow:

- `3` or more

## 8. Test the full system through api-gateway

### 8.1 Login through gateway

```powershell
$gatewayAdminLogin = Invoke-RestMethod `
  -Uri http://localhost:8080/api/auth/login `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"email":"admin@event.local","password":"Admin12345"}'
```

Expected output:

- bearer token JSON from auth-service routed through the gateway

### 8.2 List events through gateway

```powershell
$gatewayHeaders = @{ Authorization = "Bearer $($gatewayAdminLogin.accessToken)" }
Invoke-RestMethod `
  -Uri http://localhost:8080/api/events `
  -Method Get `
  -Headers $gatewayHeaders
```

Expected output:

- JSON array of events
- each event includes:
  - `id`
  - `title`
  - `status`
  - `registeredCount`
  - `availableSeats`

### 8.3 Check availability through gateway

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/events/$($event.id)/availability" `
  -Method Get `
  -Headers $gatewayHeaders
```

Expected output pattern:

```json
{
  "eventId": 7,
  "registrationOpen": true
}
```

## 9. Test fail-closed behavior

Stop only the registration service:

```powershell
docker compose stop registration-service
```

Then call availability on an existing event:

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8082/api/events/$($event.id)/availability" `
  -Method Get `
  -Headers $adminHeaders
```

Expected result:

- HTTP status: `503`
- JSON body pattern:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Registration count is unavailable for event 7"
}
```

Bring the service back:

```powershell
docker compose up -d registration-service
```

## 10. Tested results from my run

I personally verified these runtime results in this workspace:

- all actuator health endpoints returned `UP`
- full Docker Compose stack started successfully
- direct auth/event/registration/notification workflow succeeded
- cancel then re-register succeeded
- registration count stayed correct after re-registration
- gateway login and gateway event access succeeded
- fail-closed check returned `503` once `registration-service` was stopped

One environment-specific note from my run:

- because the existing Docker volume already contained an older admin row, the working admin password in the live test environment was `Admin12345`
- on a fresh database created from the current Compose file, the expected demo password is `ChangeMeNow!123`
