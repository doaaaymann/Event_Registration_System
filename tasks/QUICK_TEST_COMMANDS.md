# Quick Reference Commands for Notification Service Testing

This file contains all commands you can copy-paste directly into PowerShell.

---

## Quick Start (Copy-Paste All Commands In Order)

### 1. Start Services
```powershell
cd C:\Users\asus\OneDrive\Documents\GitHub\Event_Registration_System
docker-compose up -d
docker-compose ps
```

**Expected**: All 8 services showing "Up" status

---

### 2. Register User
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method POST -Body '{"fullName":"Test User","email":"test@example.com","password":"Password123","role":"PARTICIPANT"}' -ContentType "application/json" -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**:
```json
{"id":8,"fullName":"Test User","email":"test@example.com","status":"ACTIVE","roles":["PARTICIPANT"]}
```

**Note**: Remember the `id` value (should be 8)

---

### 3. Login to Get Token
```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body '{"email":"test@example.com","password":"Password123"}' -ContentType "application/json" -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
$token = $json.accessToken
Write-Host "Token saved: $($token.Substring(0, 50))..."
```

**Expected**: Token variable is now set with your JWT

---

## Test Commands (Use After Getting Token Above)

### 4. Create Notification
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications" -Method POST -Body '{"userId":8,"type":"test","title":"Test Notification","message":"This is a test"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**:
```json
{"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":false,"createdAt":"2026-04-26T15:34:23.589159"}
```

**Note**: Remember notification `id` (should be 23)

---

### 5. Get All User Notifications
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/8" -Method GET -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: Array of notifications
```json
[{"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":false,"createdAt":"2026-04-26T15:34:23.589159"}]
```

---

### 6. Mark Notification as Read
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/23/read" -Method PATCH -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: Same notification but with `"read":true`
```json
{"id":23,"userId":8,"type":"test","title":"Test Notification","message":"This is a test","read":true,"createdAt":"2026-04-26T15:34:23.589159"}
```

---

### 7. Test Registration-Created Trigger (From Registration Service)
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-created" -Method POST -Body '{"userId":8,"type":"registration","title":"Registration Confirmed","message":"Your registration is confirmed"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: New notification created
```json
[{"id":24,"userId":8,"type":"registration","title":"Registration Confirmed","message":"Your registration is confirmed","read":false,"createdAt":"2026-04-26T15:34:51.841461"}]
```

---

### 8. Test Registration-Cancelled Trigger (From Registration Service)
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/registration-cancelled" -Method POST -Body '{"userId":8,"type":"cancellation","title":"Registration Cancelled","message":"Your registration has been cancelled"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: New notification with cancellation message
```json
[{"id":25,"userId":8,"type":"cancellation","title":"Registration Cancelled","message":"Your registration has been cancelled","read":false,"createdAt":"..."}]
```

---

### 9. Test Event-Cancelled Trigger (From Event Service)
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-cancelled" -Method POST -Body '{"userIds":[8],"type":"event_cancelled","title":"Event Cancelled","message":"Event has been cancelled"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: New notification for the user
```json
[{"id":26,"userId":8,"type":"event_cancelled","title":"Event Cancelled","message":"Event has been cancelled","read":false,"createdAt":"..."}]
```

**Note**: Uses `userIds` (array) instead of `userId` to notify multiple users

---

### 10. Test Event-Rescheduled Trigger (From Event Service)
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/internal/event-rescheduled" -Method POST -Body '{"userIds":[8],"type":"event_rescheduled","title":"Event Rescheduled","message":"Event has been rescheduled to a new date"}' -ContentType "application/json" -Headers $headers -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Expected Output**: New rescheduling notification
```json
[{"id":27,"userId":8,"type":"event_rescheduled","title":"Event Rescheduled","message":"Event has been rescheduled to a new date","read":false,"createdAt":"..."}]
```

---

### 11. Verify All Notifications Created
```powershell
$headers = @{ "Authorization" = "Bearer $token" }
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/notifications/users/8" -Method GET -Headers $headers -UseBasicParsing
$notifications = $response.Content | ConvertFrom-Json
Write-Host "Total notifications: $($notifications.Count)"
$notifications | ForEach-Object { Write-Host "ID: $($_.id), Type: $($_.type), Title: $($_.title), Read: $($_.read)" }
```

**Expected Output**:
```
Total notifications: 5
ID: 27, Type: event_rescheduled, Title: Event Rescheduled, Read: False
ID: 26, Type: event_cancelled, Title: Event Cancelled, Read: False
ID: 25, Type: cancellation, Title: Registration Cancelled, Read: False
ID: 24, Type: registration, Title: Registration Confirmed, Read: False
ID: 23, Type: test, Title: Test Notification, Read: True
```

---

## Debugging Commands

### Check if Services Are Running
```powershell
docker-compose ps
```

### View Notification Service Logs (Last 50 Lines)
```powershell
docker-compose logs notification-service -n 50
```

### View Auth Service Logs (Last 50 Lines)
```powershell
docker-compose logs auth-service -n 50
```

### View API Gateway Logs (Last 50 Lines)
```powershell
docker-compose logs api-gateway -n 50
```

### See Full Notification Service Logs
```powershell
docker-compose logs notification-service | Select-Object -Last 100
```

### Stop All Services
```powershell
docker-compose down
```

### Stop All Services and Remove Data
```powershell
docker-compose down -v
```

### Restart Services
```powershell
docker-compose restart
```

---

## Most Common Issues

### "Cannot connect to localhost:8080"
```powershell
# Services not running, start them:
docker-compose up -d
# Wait 30 seconds for them to start
Start-Sleep -Seconds 30
```

### "401 Unauthorized"
```powershell
# Token expired or invalid, re-login
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body '{"email":"test@example.com","password":"Password123"}' -ContentType "application/json" -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
$token = $json.accessToken
```

### "400 Bad Request" on register
```powershell
# Password doesn't meet requirements, use this format:
# - At least 8 characters
# - At least one uppercase letter
# - At least one lowercase letter
# - At least one digit
# Example: Password123
```

### "404 Not Found"
```powershell
# Notification or user doesn't exist
# Use the correct IDs from previous responses
# Check all previous outputs to find valid IDs
```

---

## Automated Test Script

Instead of running commands one by one, use the automated script:

```powershell
# Navigate to the tasks folder
cd C:\Users\asus\OneDrive\Documents\GitHub\Event_Registration_System\tasks

# Run the test script
powershell -ExecutionPolicy Bypass -File test-notification-service.ps1
```

This will:
1. Check if services are running ✓
2. Register a test user ✓
3. Login and get token ✓
4. Create 5 notifications (1 manual + 4 internal triggers) ✓
5. Test retrieval ✓
6. Test mark as read ✓
7. Verify all notifications stored ✓
8. Display final summary ✓

---

## Key Points to Remember

1. **User ID**: Write down the user ID from registration (usually 8 or higher)
2. **Token**: Save the token from login response, you'll use it in all requests
3. **Notification ID**: Remember notification IDs from creation responses
4. **Headers**: Always include `Authorization: Bearer {token}` for authenticated endpoints
5. **Internal Endpoints**: These are called BY other services, not by clients
   - `POST /api/notifications/internal/registration-created` - Called by registration-service
   - `POST /api/notifications/internal/registration-cancelled` - Called by registration-service
   - `POST /api/notifications/internal/event-cancelled` - Called by event-service
   - `POST /api/notifications/internal/event-rescheduled` - Called by event-service

---

## Integration Overview

```
User → Auth Service (Login) → Get JWT Token
           ↓
       API Gateway
           ↓
      Notification Service
           ↓
    PostgreSQL (notification_db)

External Services:
Registration Service → Calls Internal Notification Endpoints
Event Service → Calls Internal Notification Endpoints
```

---

## What Each Test Verifies

| Test | Verifies | Expected Result |
|------|----------|-----------------|
| Register | Auth Service working | New user created with ID |
| Login | JWT generation | Token obtained for auth |
| Create Notification | POST endpoint works | Notification saved to DB |
| Get Notifications | Retrieval works | List of notifications returned |
| Mark as Read | PATCH endpoint works | Notification status updated |
| Registration Trigger | Integration with reg-service | Notification auto-created |
| Cancellation Trigger | Integration with reg-service | Cancellation notification created |
| Event Cancelled Trigger | Integration with event-service | Event cancellation notification |
| Event Rescheduled Trigger | Integration with event-service | Reschedule notification created |
| Verify All | Data persistence | All notifications in database |

