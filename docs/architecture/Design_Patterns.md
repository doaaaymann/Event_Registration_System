# Design Patterns

This document explains the main design patterns used in the Event Registration System and connects each pattern to concrete files in the implementation.

## 1. Layered Architecture / MVC

The backend is separated into controllers, services, repositories, DTOs, entities, security classes, and exception handlers.

### Where It Appears

- `auth-service/src/main/java/com/event/authservice/controller/AuthController.java`
- `auth-service/src/main/java/com/event/authservice/service/AuthService.java`
- `auth-service/src/main/java/com/event/authservice/repository/UserRepository.java`
- `event-service/src/main/java/com/event/eventservice/controller/EventController.java`
- `event-service/src/main/java/com/event/eventservice/service/EventService.java`
- `registration-service/src/main/java/com/event/registrationservice/controller/RegistrationController.java`
- `notification-service/src/main/java/com/event/notificationservice/controller/NotificationController.java`

### Why It Is Used

Controllers handle HTTP requests and responses, services contain business rules, and repositories handle persistence. This separation makes the code easier to test, maintain, and change.

## 2. Repository Pattern

Spring Data repositories isolate database access from the service layer.

### Where It Appears

- `auth-service/src/main/java/com/event/authservice/repository/UserRepository.java`
- `auth-service/src/main/java/com/event/authservice/repository/RoleRepository.java`
- `event-service/src/main/java/com/event/eventservice/repository/EventRepository.java`
- `registration-service/src/main/java/com/event/registrationservice/repository/RegistrationRepository.java`
- `notification-service/src/main/java/com/event/notificationservice/repository/NotificationRepository.java`

### Why It Is Used

The service classes can request data using repository methods instead of writing SQL or persistence logic directly. This improves readability and keeps data access replaceable.

## 3. DTO Pattern

The project uses request and response DTOs instead of exposing entity classes directly through APIs.

### Where It Appears

- `auth-service/src/main/java/com/event/authservice/dto/request`
- `auth-service/src/main/java/com/event/authservice/dto/response`
- `event-service/src/main/java/com/event/eventservice/dto/request`
- `event-service/src/main/java/com/event/eventservice/dto/response`
- `registration-service/src/main/java/com/event/registrationservice/dto/request`
- `registration-service/src/main/java/com/event/registrationservice/dto/response`
- `notification-service/src/main/java/com/event/notificationservice/dto/request`
- `notification-service/src/main/java/com/event/notificationservice/dto/response`

### Why It Is Used

DTOs protect the internal database model, validate incoming data, and allow each API endpoint to return exactly the fields needed by the frontend.

## 4. Dependency Injection

Dependencies are injected through constructors and managed by Spring.

### Where It Appears

- Controllers receive service dependencies through constructors.
- Services receive repositories, encoders, JWT services, and service clients through constructors.
- AOP aspects and security filters are Spring-managed components.

### Why It Is Used

Dependency injection reduces hard-coded object creation, makes unit testing easier, and keeps classes focused on their own responsibility.

## 5. API Gateway Pattern

The `api-gateway` service is the single backend entry point for frontend and external clients.

### Where It Appears

- `api-gateway/src/main/resources/application.yml`
- `config-server/src/main/resources/config/api-gateway.yml`

### Why It Is Used

The frontend can call one gateway instead of knowing every service address. The gateway routes:

- `/api/auth/**` to `auth-service`
- `/api/events/**` to `event-service`
- `/api/registrations/**` to `registration-service`
- `/api/notifications/**` to `notification-service`

## 6. Service Discovery Pattern

The system uses Eureka so services can register themselves and discover other services by name.

### Where It Appears

- `eureka-server`
- service configuration files under `config-server/src/main/resources/config`
- service URLs such as `http://eureka-server:8761/eureka`

### Why It Is Used

Service discovery avoids hard-coding physical service locations. This is important in Docker, Kubernetes, and cloud environments where service instances can move or scale.

## 7. Centralized Configuration Pattern

The system uses Spring Cloud Config to store shared service configuration in one place.

### Where It Appears

- `config-server`
- `config-server/src/main/resources/config/auth-service.yml`
- `config-server/src/main/resources/config/event-service.yml`
- `config-server/src/main/resources/config/registration-service.yml`
- `config-server/src/main/resources/config/notification-service.yml`

### Why It Is Used

Centralized configuration makes environment-specific settings easier to manage and supports cloud-style deployments.

## 8. Client / Proxy Pattern

Service-to-service calls are wrapped in client classes instead of being scattered across the business logic.

### Where It Appears

- `event-service/src/main/java/com/event/eventservice/client/NotificationServiceClient.java`
- `event-service/src/main/java/com/event/eventservice/client/RegistrationServiceClient.java`
- `registration-service/src/main/java/com/event/registrationservice/client/EventServiceClient.java`
- `registration-service/src/main/java/com/event/registrationservice/client/NotificationServiceClient.java`
- `registration-service/src/main/java/com/event/registrationservice/client/UserServiceClient.java`

### Why It Is Used

Client classes provide a clean boundary for remote service communication. If the downstream service URL, API shape, or error handling changes, the update is isolated in the client class.

## 9. AOP / Cross-Cutting Concern Pattern

Logging and audit behavior are implemented with aspects rather than repeated inside every controller method.

### Where It Appears

- `auth-service/src/main/java/com/event/authservice/aop/AuthAuditAspect.java`
- `auth-service/src/main/java/com/event/authservice/aop/ControllerLoggingAspect.java`
- `event-service/src/main/java/com/event/eventservice/aop/ControllerLoggingAspect.java`
- `registration-service/src/main/java/com/event/registrationservice/aop/ControllerLoggingAspect.java`

### Why It Is Used

AOP keeps cross-cutting concerns such as logging and auditing separate from business logic. This makes controllers and services cleaner.

## 10. Front Controller Style

Each microservice exposes a focused set of REST controllers, while the API Gateway acts as the public front controller for the backend.

### Where It Appears

- `api-gateway`
- all `controller` packages

### Why It Is Used

Requests enter through a consistent HTTP layer, then flow into the correct service and business operation.

## Summary

The project uses both application-level patterns and microservices architecture patterns. The most important patterns for grading are:

- MVC / Layered Architecture
- Repository
- DTO
- Dependency Injection
- API Gateway
- Service Discovery
- Centralized Configuration
- Client / Proxy
- AOP
