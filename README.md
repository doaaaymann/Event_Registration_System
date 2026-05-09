# Event Registration System

The Event Registration System is a microservices-based Software Engineering project for managing events, participant registrations, organizer assignments, and in-system notifications. It provides a web frontend for users and a Spring Boot backend split into focused services for authentication, events, registrations, and notifications.

## What This Project Does

The platform is designed for academic events, workshops, seminars, conferences, and similar activities where users need to:

- create and manage events
- browse upcoming events
- register and cancel participation
- view confirmed tickets
- assign organizers to events
- receive notifications for important event changes

The system supports three main roles:

- `ADMIN`
- `ORGANIZER`
- `PARTICIPANT`

## Main Features

- Secure user registration and login
- JWT-based authentication and role-based authorization
- Admin-managed user creation for organizers and participants
- Event creation, update, cancellation, and rescheduling
- Multi-organizer assignment support
- Event browsing with schedule, location, status, and seat availability
- Participant registration with duplicate-registration protection
- Registration cancellation and ticket-style confirmed-registration view
- Organizer dashboard for assigned events
- In-system notifications with read/unread state
- Docker-friendly microservices setup

## Architecture Overview

This project uses a microservices architecture with a React frontend and Spring Boot backend services.

### Services

- `frontend`
  - React/Vite client application
- `api-gateway`
  - Single entry point for frontend requests
- `auth-service`
  - User accounts, login, JWT validation, roles, profile access
- `event-service`
  - Event lifecycle management and organizer assignments
- `registration-service`
  - Participant event registrations and cancellation flow
- `notification-service`
  - User notifications and internal notification triggers
- `config-server`
  - Centralized configuration
- `eureka-server`
  - Service discovery

## Role Responsibilities

### Admin

- Create events
- Edit event details
- Cancel or reschedule events
- Create managed organizer or participant accounts
- Assign organizers to events
- Supervise overall platform usage

### Organizer

- View only assigned events
- Review event details
- Monitor registered participant counts
- Receive organizer-specific notifications

### Participant

- Register a public account
- Log in and browse events
- Register for eligible events
- Cancel a registration
- View confirmed tickets
- Read notifications

## Technology Stack

### Frontend

- React
- Vite
- JavaScript
- Axios
- Tailwind-style utility CSS

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- Spring Cloud Config
- Eureka Server
- OpenFeign / service clients

### Infrastructure

- PostgreSQL
- Docker
- Docker Compose
- Maven

## Project Structure

```text
Event_Registration_System-/
├── api-gateway/
├── auth-service/
├── config-server/
├── docker/
├── docs/
├── eureka-server/
├── event-service/
├── frontend/
├── notification-service/
├── registration-service/
├── tasks/
└── pom.xml
```



### ERD



- [`docs/architecture/OCL_Constraints.md`](./docs/architecture/OCL_Constraints.md)
  - OCL-style business constraints with matching backend implementation references
- [`docs/architecture/Design_Patterns.md`](./docs/architecture/Design_Patterns.md)
  - explanation of the design patterns used in the project with implementation references

### Cloud Deployment

- [`cloud/README.md`](./cloud/README.md)
  - explains Docker Compose deployment on a cloud VM and Kubernetes deployment
- [`cloud/kubernetes/event-registration-system.yml`](./cloud/kubernetes/event-registration-system.yml)
  - Kubernetes manifest for PostgreSQL, Config Server, Eureka Server, API Gateway, and the backend domain services

## Database Model

The system stores data across separate service databases.

### Auth domain

- `users`
- `roles`
- `user_roles`

### Event domain

- `events`

### Registration domain

- `registrations`

### Notification domain

- `notifications`

Because the system is microservices-based, some relationships are logical cross-service relationships enforced in application logic instead of direct foreign keys across one shared database.

## Core Business Rules

- Public self-registration is limited to `PARTICIPANT` accounts
- Only admins can create managed organizer or participant accounts
- Every event must have at least one organizer
- Only `SCHEDULED` or `RESCHEDULED` events can accept registrations
- A participant cannot hold duplicate active registrations for the same event
- Cancelled events cannot be edited as active events
- Notifications are only readable by their owner or an admin

## Running the Project

The project is designed to run as multiple services. Typical setup includes:

1. Start the supporting infrastructure and databases
2. Start `config-server` and `eureka-server`
3. Start backend domain services
4. Start `api-gateway`
5. Start the frontend

The exact startup flow may vary depending on whether you use Docker Compose or run services individually.

## API Areas

The main API areas exposed through the gateway are:

- `/api/auth`
- `/api/events`
- `/api/registrations`
- `/api/notifications`

## Testing and Work Reports

The `tasks/` folder contains working notes, testing flows, and service-specific reports created during development. These files are helpful for internal project work but are separate from the cleaned long-term documentation under `docs/`.

## Current State

This repository contains the implemented backend and frontend for the Event Registration System, plus supporting architecture documentation. It is suitable as a course project, a portfolio-quality microservices example, and a base for future additions such as email delivery, analytics, payment-based tickets, or check-in workflows.
