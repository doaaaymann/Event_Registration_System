# Event Registration System

## Description

The Event Registration System is a Software Engineering-2 project built with Spring Boot using a microservices architecture. The system allows users to manage and participate in events such as seminars, workshops, and conferences through REST APIs.

The platform is designed to support different user roles, secure API access, and provide a scalable structure using Spring Cloud, Docker, and a database-backed backend.

## Project Overview

This system helps organizers create and manage events while allowing participants to browse and register for available events. It is intended to demonstrate core software engineering concepts including modular design, role-based authorization, Aspect-Oriented Programming, microservices, and software documentation.

### Target users

- Event Organizer
- Participant
- Admin

## Core Features

- User registration and authentication
- Role-based authorization for API access
- Create events with limited seats
- View upcoming events
- Register for an event
- Track registered participants
- Cancel events
- Reschedule events
- Notification-ready architecture for event and registration updates

## Project Requirements

This project is developed according to the Software Engineering-2 guidelines:

- Spring Boot is used as the backend framework
- REST APIs are used for system functionality
- The application is organized into at least four functional modules
- Different user roles are supported with controlled API authorization
- Aspect-Oriented Programming (AOP) is included
- The application is Dockerized
- Microservices and Spring Cloud are used
- A database is included
- User registration and authentication are implemented
- A complete Software Requirements Specification (SRS) document is required
- Essential diagrams are required, including:
- Use Case Diagram
- Class Diagram
- Sequence Diagrams
- Activity Diagrams

## General Project Structure

The project follows a microservices-based structure. Each service has a clear responsibility and communicates through APIs.

### Main parts of the system

- `config-server`
  Provides centralized configuration for all microservices.

- `eureka-server`
  Handles service discovery between microservices.

- `api-gateway`
  Serves as the single entry point for external client requests and routes them to the correct services.

- `auth-service`
  Manages user registration, login, authentication, JWT handling, and role-based access control.

- `event-service`
  Manages event creation, event updates, seat limits, cancellation, and rescheduling.

- `registration-service`
  Handles participant registration, cancellation of registration, and participant tracking.

- `notification-service`
  Can be used for sending or storing notifications related to registrations, cancellations, or schedule changes.

- `database`
  Stores application data for users, events, registrations, and notifications.

## Suggested Functional Modules

The system can be viewed as four main business modules:

1. Authentication and User Management
2. Event Management
3. Registration Management
4. Notification Management

## Roles in the System

### Admin

- Manage overall system access
- Monitor users and permissions
- Support administrative control over the platform

### Event Organizer

- Create events
- Update event details
- Cancel events
- Reschedule events
- View participant lists

### Participant

- Register an account
- Log in to the system
- View available and upcoming events
- Register for an event
- Cancel a registration

## Technology Stack

- Java
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Cloud
- Eureka Server
- Spring Cloud Gateway
- Spring Cloud Config
- PostgreSQL or another relational database
- Docker
- Maven
- AOP with Spring

## API Design

The system is API-based and exposes REST endpoints for the main operations. These APIs are expected to cover:

- authentication
- user management
- event management
- event registration
- participant tracking
- notification handling

## Security

The system includes:

- user registration
- user login
- authentication logic
- authorization based on roles
- protected APIs depending on user permissions

JWT can be used to secure communication between clients and backend services.

## Dockerization

The application is designed to be Dockerized for easy setup and deployment. Each microservice can run in its own container, and supporting services such as the database can also be managed with Docker Compose.

## Database

A database is required to persist system data such as:

- users
- roles
- events
- registrations
- notifications
