# Auth Service Work And Testing Guide

This guide is for Person 1, the owner of `auth-service`.

It explains:

- how to open the project
- how to run only the auth-related services
- how to work inside your part safely
- how to run the auth tests
- how to test the auth APIs in Postman
- how to decide that your part is finished

## 1. Open The Project

Open a terminal in the project root and move into the repository:

```powershell
cd C:\Users\doaaa\Downloads\event-registration-system-full
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

## 2. What Person 1 Owns

Person 1 owns only the authentication microservice.

You should mainly work inside:

- `auth-service/src/main/java`
- `auth-service/src/main/resources`
- `auth-service/src/test/java`
- `auth-service/pom.xml`

Avoid editing:

- `event-service`
- `registration-service`
- `notification-service`
- `api-gateway`
- `config-server`
- `eureka-server`
- `docker-compose.yml`

unless the team agrees first.

## 3. What Person 1 Is Responsible For

According to the task plan, Person 1 is responsible for:

- user registration
- login
- JWT generation
- JWT validation
- role-based authorization basics
- auth database tables
- auth tests

## 4. Run Only The Auth Stack With Docker

If you only want the services needed for auth testing, run:

```powershell
docker compose up --build postgres config-server eureka-server auth-service
```

This starts:

- PostgreSQL
- Config Server
- Eureka Server
- Auth Service

To stop the containers:

```powershell
docker compose down
```

To watch only auth-service logs:

```powershell
docker compose logs -f auth-service
```

## 5. Useful URLs

Direct auth-service base URL:

```text
http://localhost:8081
```

If later you want to test through the API Gateway:

```text
http://localhost:8080
```

For Person 1 testing, use the direct auth-service URL first:

```text
http://localhost:8081
```

## 6. How To Work On Your Part

Before changing code:

1. go to the root project folder
2. make sure the auth stack can run
3. work only inside `auth-service`
4. after changes, run auth tests
5. then test manually in Postman

Recommended local workflow:

```powershell
cd C:\Users\doaaa\Downloads\event-registration-system-full
```

```powershell
mvn -pl auth-service test
```

If tests pass, run the auth stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service
```

Then verify the endpoints in Postman.

## 7. How To Run The Auth Tests

Run all auth-service tests:

```powershell
mvn -pl auth-service test
```

Run only service tests:

```powershell
mvn -pl auth-service -Dtest=AuthServiceTest test
```

Run only controller tests:

```powershell
mvn -pl auth-service -Dtest=AuthControllerTest test
```

Expected result:

- build success
- all auth tests pass

## 8. Default Admin Account

The auth service seeds a default admin account.

Use:

- email: `admin@event.local`
- password: `EventAdmin123!`

## 9. Postman Setup

Create a Postman environment and add these variables:

- `baseUrl = http://localhost:8081`
- `token =`
- `participantToken =`
- `participantId =`

## 10. Important Postman Rules

- Always use `{{baseUrl}}`
- For protected endpoints, send `Authorization: Bearer <token>`
- Use `/users/2`, not `/users/{2}`
- Test `auth-service` directly before testing through the gateway

## 11. Postman Test Flow

### Request 1: Login Admin

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/login`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "email": "admin@event.local",
  "password": "EventAdmin123!"
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
pm.environment.set("token", json.accessToken);
```

### Request 2: Register Participant

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/register`

Headers:

- `Content-Type: application/json`

Body:

```json
{
  "fullName": "Ali Hassan",
  "email": "ali@example.com",
  "password": "Secret123",
  "role": "PARTICIPANT"
}
```

Expected:

- `201 Created`

Example response:

```json
{
  "id": 2,
  "fullName": "Ali Hassan",
  "email": "ali@example.com",
  "status": "ACTIVE",
  "roles": ["PARTICIPANT"]
}
```

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("participantId", json.id);
```

### Request 3: Reject Public Organizer Registration

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/register`

Body:

```json
{
  "fullName": "Omar Organizer",
  "email": "omar@example.com",
  "password": "Secret123",
  "role": "ORGANIZER"
}
```

Expected:

- `400 Bad Request`

### Request 4: Login Participant

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/login`

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

Postman `Tests` tab:

```javascript
const json = pm.response.json();
pm.environment.set("participantToken", json.accessToken);
```

### Request 4B: Create Organizer As Admin

- Method: `POST`
- URL: `{{baseUrl}}/api/auth/admin/users`

Headers:

- `Authorization: Bearer {{token}}`
- `Content-Type: application/json`

Body:

```json
{
  "fullName": "Omar Organizer",
  "email": "omar@example.com",
  "password": "Secret123",
  "role": "ORGANIZER"
}
```

Expected:

- `201 Created`

### Request 5: Get Current User

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/me`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

### Request 6: Validate Participant Token

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/validate`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

Expected response shape:

```json
{
  "valid": true,
  "userId": 2,
  "email": "ali@example.com",
  "roles": ["PARTICIPANT"]
}
```

### Request 7: Get Participant By Own ID

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/users/{{participantId}}`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

### Request 8: Get Participant Roles By Own ID

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/users/{{participantId}}/roles`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `200 OK`

### Request 9: Try Forbidden Access As Participant

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/users/1`

Headers:

- `Authorization: Bearer {{participantToken}}`

Expected:

- `403 Forbidden`

### Request 10: Get Participant As Admin

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/users/{{participantId}}`

Headers:

- `Authorization: Bearer {{token}}`

Expected:

- `200 OK`

### Request 11: Get Roles As Admin

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/users/{{participantId}}/roles`

Headers:

- `Authorization: Bearer {{token}}`

Expected:

- `200 OK`

### Request 12: Unauthorized Without Token

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/me`

No Authorization header.

Expected:

- `401 Unauthorized`

### Request 13: Unauthorized With Invalid Token

- Method: `GET`
- URL: `{{baseUrl}}/api/auth/me`

Headers:

- `Authorization: Bearer abc.def.ghi`

Expected:

- `401 Unauthorized`

## 12. Recommended Postman Order

1. Login admin
2. Register participant
3. Reject organizer public registration
4. Login participant
5. Create organizer as admin
6. Get current user
7. Validate token
8. Get participant by own id
9. Get participant roles by own id
10. Try forbidden access
11. Use admin to access participant data
12. Test missing token
13. Test invalid token

## 13. If Something Fails

First, look at auth-service logs:

```powershell
docker compose logs -f auth-service
```

Then check:

- did you use `{{baseUrl}} = http://localhost:8081`
- did you send `Authorization: Bearer <token>`
- did you use `/users/2` and not `/users/{2}`
- did you rebuild after code changes
- did you use the latest token after login

## 14. How Person 1 Knows The Auth Part Is Done

Person 1 can mark the auth task done when all of these are true:

- auth-service starts successfully
- `mvn -pl auth-service test` passes
- register works
- login works
- `/me` works
- `/validate` works
- `/users/{userId}` works
- `/users/{userId}/roles` works
- participant cannot access another user
- admin can access users and roles
- public organizer registration is rejected
- admin can create organizer users through `/api/auth/admin/users`

## 15. Fast Final Checklist

Run tests:

```powershell
mvn -pl auth-service test
```

Run auth stack:

```powershell
docker compose up --build postgres config-server eureka-server auth-service
```

Then complete the Postman flow above.

If all expected results match, Person 1 is ready to report the auth-service task as complete.
