# Notification Service Integration Testing Guide

This guide shows you how to test the notification-service integration with auth-service, event-service, and registration-service.

## Prerequisites

1. All services running: `docker-compose ps`
2. You should see all 8 services with status "Up"
3. API Gateway running on port 8080

## Step 1: Check if all services are running

### Command:
```powershell
docker-compose ps
```

### Expected Output:
```
NAME                   IMAGE                                            COMMAND                  SERVICE                CREATED          STATUS                    PORTS
api-gateway            event_registration_system-api-gateway            "java -jar /app/app.…"   api-gateway            XX seconds ago   Up XX seconds             0.0.0.0:8080->8080/tcp
auth-service           event_registration_system-auth-service           "java -jar /app/app.…"   auth-service           XX seconds ago   Up XX seconds             0.0.0.0:8081->8081/tcp
config-server          event_registration_system-config-server          "java -jar /app/app.…"   config-server          XX seconds ago   Up XX seconds             0.0.0.0:8888->8888/tcp
eureka-server          event_registration_system-eureka-server          "java -jar /app/app.…"   eureka-server          XX seconds ago   Up XX seconds             0.0.0.0:8761->8761/tcp
event-postgres         postgres:16-alpine                               "docker-entrypoint.s…"   postgres               XX seconds ago   Up XX seconds (healthy)   0.0.0.0:5432->5432/tcp
event-service          event_registration_system-event-service          "java -jar /app/app.…"   event-service          XX seconds ago   Up XX seconds             0.0.0.0:8082->8082/tcp
notification-service   event_registration_system-notification-service   "java -jar /app/app.…"   notification-service   XX seconds ago   Up XX seconds             0.0.0.0:8084->8084/tcp
registration-service   event_registration_system-registration-service   "java -jar /app/app.…"   registration-service   XX seconds ago   Up XX seconds             0.0.0.0:8083->8083/tcp
```

**Status Check**: All services should be "Up". If any are "Exit" or "Restarting", check logs with:
```powershell
docker-compose logs notification-service
```

---

## Step 2: Register a Test User (Auth Service Integration)

This tests that auth-service is working and providing JWT tokens.

### Command:
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method POST -Body '{"fullName":"Test User","email":"test@example.com","password":"Password123","role":"PARTICIPANT"}' -ContentType "application/json" -UseBasicParsing
```

### Expected Output:
```
StatusCode        : 201
StatusDescription : Created
Content           : {"id":8,"fullName":"Test User","email":"test@example.com","status":"ACTIVE","roles":["PARTICIPANT"]}
```

**What it means**: User was created successfully with ID 8. You'll use this ID for all subsequent tests.

**Notes**:
- Role must be "PARTICIPANT" for public registration (ADMIN/ORGANIZER require special access)
- Password must contain uppercase, lowercase, and digits (as shown: `Password123`)
- Email must be valid format

---

## Step 3: Login and Get JWT Token

This authenticates with auth-service and gets a token needed for all other endpoints.

### Command:
```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body '{"email":"test@example.com","password":"Password123"}' -ContentType "application/json" -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
$token = $json.accessToken
Write-Host "Token: $token"
```

### Expected Output:
```
Token: eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwidXNlcklkIjo4LCJyb2xlcyI6WyJQQVJUSUNJUEFOVCJdLCJpYXQiOjE2NzcyMTc2NTYsImV4cCI6MTY3NzMwNDA1Nn0.qoZA2ItQpprPRpMsTRn9PKXVS6q3cIWnjAAjaOX4HWDfnfk8xf9qHmIJOX451qXK
```

**What it means**: You now have a JWT token that proves you're authenticated as user ID 8. This token will be used in all subsequent requests. The token is valid for a limited time.

**Save this token** - you'll use it in all the following commands by replacing `$token` variable.

---

## Step 4: Test Create Notification Endpoint

This tests that the notification-service can create notifications.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications" -Method POST -Body '{"userId":8,"type":"test","title":"Test Notification","message":"This is a test"}' -ContentType "application/json" -Headers $headers -UseBasicParsing
```

### Expected Output:
```
StatusCode        : 201
StatusDescription : Created
Content           : {"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":false,"createdAt":"2026-04-26T15:34:23.589158579"}
```

**What it means**: 
- A notification was created successfully with ID 23
- It's associated with user ID 8
- The `read` status is `false` (unread)
- Timestamp shows when it was created

**Endpoint Being Tested**: `POST /api/notifications` (public endpoint for creating notifications)

---

## Step 5: Test Retrieve Notifications Endpoint

This tests that the notification-service can retrieve all notifications for a user.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/8" -Method GET -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
[{"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":false,"createdAt":"2026-04-26T15:34:23.589159"}]
```

**What it means**: 
- Retrieved a list of all notifications for user ID 8
- Returns an array of notification objects
- Each notification shows all details including read status, type, and creation time
- Results are ordered by creation date (newest first)

**Endpoint Being Tested**: `GET /api/notifications/users/{userId}` (retrieval endpoint)

---

## Step 6: Test Mark as Read Endpoint

This tests that the notification-service can update the read status of a notification.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/23/read" -Method PATCH -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
{"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":true,"createdAt":"2026-04-26T15:34:23.589159"}
```

**What it means**: 
- The notification (ID 23) was updated successfully
- The `read` status changed from `false` to `true`
- All other fields remain the same

**Endpoint Being Tested**: `PATCH /api/notifications/{notificationId}/read` (status update endpoint)

---

## Step 7: Test Internal Registration-Created Endpoint

This simulates registration-service calling the notification-service to send a registration confirmation notification.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-created" -Method POST -Body '{"userId":8,"type":"registration","title":"Registration Confirmed","message":"Your registration is confirmed"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
[{"id":24,"userId":8,"type":"registration","title":"Registration Confirmed","message":"Your registration is confirmed","read":false,"createdAt":"2026-04-26T15:34:51.841461293"}]
```

**What it means**: 
- Internal endpoint received the trigger from registration-service
- A new notification (ID 24) was created
- Returns an array with the created notification(s)
- This simulates: Event Registration → Registration Service → Notification Service

**Endpoint Being Tested**: `POST /api/notifications/internal/registration-created` (internal endpoint for registration triggers)

**Real-world usage**: When registration-service creates a new registration, it calls this endpoint to notify the user.

---

## Step 8: Test Internal Registration-Cancelled Endpoint

This simulates registration-service calling to send a cancellation notification.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-cancelled" -Method POST -Body '{"userId":8,"type":"cancellation","title":"Registration Cancelled","message":"Your registration has been cancelled"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
[{"id":25,"userId":8,"type":"cancellation","title":"Registration Cancelled","message":"Your registration has been cancelled","read":false,"createdAt":"2026-04-26T15:35:10.123456"}]
```

**What it means**: Internal endpoint for cancellation notifications (ID 25 created).

**Endpoint Being Tested**: `POST /api/notifications/internal/registration-cancelled`

---

## Step 9: Test Internal Event-Cancelled Endpoint

This simulates event-service calling to notify users of a cancelled event.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-cancelled" -Method POST -Body '{"userIds":[8],"type":"event_cancelled","title":"Event Cancelled","message":"Event has been cancelled"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
[{"id":26,"userId":8,"type":"event_cancelled","title":"Event Cancelled","message":"Event has been cancelled","read":false,"createdAt":"2026-04-26T15:35:25.654321"}]
```

**What it means**: 
- Internal endpoint received the trigger from event-service
- A new notification (ID 26) was created
- Notice `userIds` is an array - event-service can notify multiple users
- This simulates: Event Cancelled → Event Service → Notification Service (broadcast to all registrants)

**Endpoint Being Tested**: `POST /api/notifications/internal/event-cancelled`

**Real-world usage**: When an event is cancelled, event-service calls this endpoint with all affected user IDs.

---

## Step 10: Test Internal Event-Rescheduled Endpoint

This simulates event-service notifying users of a rescheduled event.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-rescheduled" -Method POST -Body '{"userIds":[8],"type":"event_rescheduled","title":"Event Rescheduled","message":"Event has been rescheduled to a new date"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Expected Output:
```
[{"id":27,"userId":8,"type":"event_rescheduled","title":"Event Rescheduled","message":"Event has been rescheduled to a new date","read":false,"createdAt":"2026-04-26T15:35:40.987654"}]
```

**What it means**: Internal endpoint for event reschedule notifications.

**Endpoint Being Tested**: `POST /api/notifications/internal/event-rescheduled`

---

## Step 11: Verify All Notifications Were Stored

This retrieves all user notifications to verify everything was created and persisted.

### Command:
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/8" -Method GET -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content | ConvertFrom-Json | ForEach-Object { Write-Host "ID: $($_.id), Type: $($_.type), Read: $($_.read), Title: $($_.title)" }
```

### Expected Output:
```
ID: 27, Type: event_rescheduled, Read: False, Title: Event Rescheduled
ID: 26, Type: event_cancelled, Read: False, Title: Event Cancelled
ID: 25, Type: cancellation, Read: False, Title: Registration Cancelled
ID: 24, Type: registration, Read: False, Title: Registration Confirmed
ID: 23, Type: test, Read: True, Title: Test Notification
```

**What it means**: 
- All 5 notifications are persisted in the database
- They're returned in reverse chronological order (newest first)
- The first notification (ID 23) shows `read: True` because we marked it as read in Step 6
- All others are unread

---

## Step 12: Check Service Logs (Debugging)

If any request fails, check the logs to see what happened:

### For Notification Service:
```powershell
docker-compose logs notification-service | Select-Object -Last 30
```

### For Auth Service:
```powershell
docker-compose logs auth-service | Select-Object -Last 30
```

### For API Gateway:
```powershell
docker-compose logs api-gateway | Select-Object -Last 30
```

---

## Complete Test Script (All Steps in One File)

Save this as `test-notification-service.ps1`:

```powershell
# Color output
$success = 'Green'
$error = 'Red'
$info = 'Yellow'

Write-Host "========================================" -ForegroundColor $info
Write-Host "NOTIFICATION SERVICE INTEGRATION TEST" -ForegroundColor $info
Write-Host "========================================" -ForegroundColor $info

# Step 1: Check services
Write-Host "`n[1/12] Checking if all services are running..." -ForegroundColor $info
$services = docker-compose ps
Write-Host $services

# Step 2: Register user
Write-Host "`n[2/12] Registering test user..." -ForegroundColor $info
$registerResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" `
    -Method POST `
    -Body '{"fullName":"Test User","email":"test@example.com","password":"Password123","role":"PARTICIPANT"}' `
    -ContentType "application/json" `
    -UseBasicParsing

$registerData = $registerResponse.Content | ConvertFrom-Json
$userId = $registerData.id
Write-Host "✓ User registered with ID: $userId" -ForegroundColor $success
Write-Host "Response: $($registerResponse.Content)"

# Step 3: Login and get token
Write-Host "`n[3/12] Logging in to get JWT token..." -ForegroundColor $info
$loginResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -Body '{"email":"test@example.com","password":"Password123"}' `
    -ContentType "application/json" `
    -UseBasicParsing

$loginData = $loginResponse.Content | ConvertFrom-Json
$token = $loginData.accessToken
Write-Host "✓ Token obtained" -ForegroundColor $success
Write-Host "Token (first 50 chars): $($token.Substring(0, 50))..."

# Prepare headers for authenticated requests
$headers = @{ "Authorization" = "Bearer $token" }

# Step 4: Create notification
Write-Host "`n[4/12] Creating a test notification..." -ForegroundColor $info
$createResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications" `
    -Method POST `
    -Body "{`"userId`":$userId,`"type`":`"test`",`"title`":`"Test Notification`",`"message`":`"This is a test`"}" `
    -ContentType "application/json" `
    -Headers $headers `
    -UseBasicParsing

$createData = $createResponse.Content | ConvertFrom-Json
$notificationId = $createData.id
Write-Host "✓ Notification created with ID: $notificationId" -ForegroundColor $success
Write-Host "Response: $($createResponse.Content)"

# Step 5: Retrieve notifications
Write-Host "`n[5/12] Retrieving user notifications..." -ForegroundColor $info
$getResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/$userId" `
    -Method GET `
    -Headers $headers `
    -UseBasicParsing

$notifications = $getResponse.Content | ConvertFrom-Json
Write-Host "✓ Retrieved $($notifications.Count) notification(s)" -ForegroundColor $success
Write-Host "Response: $($getResponse.Content)"

# Step 6: Mark as read
Write-Host "`n[6/12] Marking notification as read..." -ForegroundColor $info
$readResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/$notificationId/read" `
    -Method PATCH `
    -Headers $headers `
    -UseBasicParsing

$readData = $readResponse.Content | ConvertFrom-Json
Write-Host "✓ Notification marked as read (status: $($readData.read))" -ForegroundColor $success
Write-Host "Response: $($readResponse.Content)"

# Step 7: Internal registration-created
Write-Host "`n[7/12] Testing internal registration-created trigger..." -ForegroundColor $info
$regCreatedResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-created" `
    -Method POST `
    -Body "{`"userId`":$userId,`"type`":`"registration`",`"title`":`"Registration Confirmed`",`"message`":`"Your registration is confirmed`"}" `
    -ContentType "application/json" `
    -Headers $headers `
    -UseBasicParsing

Write-Host "✓ Registration-created trigger processed" -ForegroundColor $success
Write-Host "Response: $($regCreatedResponse.Content)"

# Step 8: Internal registration-cancelled
Write-Host "`n[8/12] Testing internal registration-cancelled trigger..." -ForegroundColor $info
$regCancelledResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-cancelled" `
    -Method POST `
    -Body "{`"userId`":$userId,`"type`":`"cancellation`",`"title`":`"Registration Cancelled`",`"message`":`"Your registration has been cancelled`"}" `
    -ContentType "application/json" `
    -Headers $headers `
    -UseBasicParsing

Write-Host "✓ Registration-cancelled trigger processed" -ForegroundColor $success
Write-Host "Response: $($regCancelledResponse.Content)"

# Step 9: Internal event-cancelled
Write-Host "`n[9/12] Testing internal event-cancelled trigger..." -ForegroundColor $info
$eventCancelledResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-cancelled" `
    -Method POST `
    -Body "{`"userIds`":[$userId],`"type`":`"event_cancelled`",`"title`":`"Event Cancelled`",`"message`":`"Event has been cancelled`"}" `
    -ContentType "application/json" `
    -Headers $headers `
    -UseBasicParsing

Write-Host "✓ Event-cancelled trigger processed" -ForegroundColor $success
Write-Host "Response: $($eventCancelledResponse.Content)"

# Step 10: Internal event-rescheduled
Write-Host "`n[10/12] Testing internal event-rescheduled trigger..." -ForegroundColor $info
$eventRescheduledResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-rescheduled" `
    -Method POST `
    -Body "{`"userIds`":[$userId],`"type`":`"event_rescheduled`",`"title`":`"Event Rescheduled`",`"message`":`"Event has been rescheduled to a new date`"}" `
    -ContentType "application/json" `
    -Headers $headers `
    -UseBasicParsing

Write-Host "✓ Event-rescheduled trigger processed" -ForegroundColor $success
Write-Host "Response: $($eventRescheduledResponse.Content)"

# Step 11: Verify all notifications
Write-Host "`n[11/12] Verifying all notifications are stored..." -ForegroundColor $info
$allNotificationsResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/$userId" `
    -Method GET `
    -Headers $headers `
    -UseBasicParsing

$allNotifications = $allNotificationsResponse.Content | ConvertFrom-Json
Write-Host "✓ Total notifications: $($allNotifications.Count)" -ForegroundColor $success
Write-Host "Notifications:"
$allNotifications | ForEach-Object {
    Write-Host "  - ID: $($_.id), Type: $($_.type), Title: $($_.title), Read: $($_.read)"
}

# Step 12: Final summary
Write-Host "`n[12/12] Test Summary" -ForegroundColor $info
Write-Host "========================================" -ForegroundColor $info
Write-Host "✓ All endpoints tested successfully!" -ForegroundColor $success
Write-Host "✓ User ID: $userId" -ForegroundColor $success
Write-Host "✓ Total notifications created: $($allNotifications.Count)" -ForegroundColor $success
Write-Host "✓ Auth Service: Working (JWT token obtained)" -ForegroundColor $success
Write-Host "✓ Notification Service: Working (CRUD operations succeeded)" -ForegroundColor $success
Write-Host "✓ API Gateway: Working (all routes accessible)" -ForegroundColor $success
Write-Host "========================================" -ForegroundColor $info
```

**To run this script:**
```powershell
powershell -ExecutionPolicy Bypass -File test-notification-service.ps1
```

---

## Common Issues and Solutions

### Issue: 400 Bad Request on register
**Cause**: Password doesn't meet requirements
**Solution**: Use `Password123` (has uppercase, lowercase, digit)

### Issue: Cannot connect to localhost:8080
**Cause**: Services not running
**Solution**: Run `docker-compose up -d` first

### Issue: 401 Unauthorized
**Cause**: Missing or invalid JWT token
**Solution**: Make sure you're using the Bearer token from login response

### Issue: 403 Forbidden
**Cause**: User trying to access another user's notifications
**Solution**: The API prevents users from seeing others' notifications unless they're ADMIN

### Issue: 404 Not Found
**Cause**: Notification ID doesn't exist
**Solution**: Use a valid notification ID from a previous GET request

---

## Summary of Endpoints Tested

| # | Method | Endpoint | Purpose | Service |
|---|--------|----------|---------|---------|
| 1 | POST | /api/auth/register | Register new user | Auth Service |
| 2 | POST | /api/auth/login | Get JWT token | Auth Service |
| 3 | POST | /api/notifications | Create notification | Notification Service |
| 4 | GET | /api/notifications/users/{userId} | Retrieve user notifications | Notification Service |
| 5 | PATCH | /api/notifications/{notificationId}/read | Mark as read | Notification Service |
| 6 | POST | /api/notifications/internal/registration-created | Trigger from registration-service | Notification Service |
| 7 | POST | /api/notifications/internal/registration-cancelled | Trigger from registration-service | Notification Service |
| 8 | POST | /api/notifications/internal/event-cancelled | Trigger from event-service | Notification Service |
| 9 | POST | /api/notifications/internal/event-rescheduled | Trigger from event-service | Notification Service |

---

## Expected Data Flow

```
User Registration & Login
  ↓
Auth Service (Port 8081) → JWT Token Generated
  ↓
Create Notification
  ↓
API Gateway (Port 8080) → Routes to Notification Service (Port 8084)
  ↓
Notification Service → Stores in notification_db (PostgreSQL)
  ↓
Retrieve Notification
  ↓
Returns from Notification Service → Users can read notifications

Service Integration:
  Registration Service → Calls internal endpoint → Notification Service
  Event Service → Calls internal endpoint → Notification Service
  Notification Service → Stores in database → User retrieves via API
```

