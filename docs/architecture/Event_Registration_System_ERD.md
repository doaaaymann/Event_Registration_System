# Event Registration System ERD

This ERD models the latest database structure from service migrations.  
Because this project uses microservices with separate databases, relationships across services are **logical** (application-level) and not enforced with cross-database foreign keys.

## Mermaid Source

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR status
        VARCHAR interests
        TIMESTAMP created_at
    }

    ROLES {
        BIGINT id PK
        VARCHAR name UK
    }

    USER_ROLES {
        BIGINT user_id PK, FK
        BIGINT role_id PK, FK
    }

    EVENTS {
        BIGINT id PK
        VARCHAR title
        TEXT description
        VARCHAR location
        TIMESTAMP start_time
        TIMESTAMP end_time
        INTEGER max_seats
        VARCHAR status
        BIGINT organizer_id
        TEXT organizer_ids
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    REGISTRATIONS {
        BIGINT id PK
        BIGINT event_id
        BIGINT participant_id
        VARCHAR status
        TIMESTAMP registered_at
        TIMESTAMP cancelled_at
    }

    NOTIFICATIONS {
        BIGINT id PK
        BIGINT user_id
        VARCHAR type
        VARCHAR title
        TEXT message
        BOOLEAN read
        TIMESTAMP created_at
    }

    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : grants

    USERS ||--o{ EVENTS : organizes_logically
    USERS ||--o{ REGISTRATIONS : participates_logically
    EVENTS ||--o{ REGISTRATIONS : receives
    USERS ||--o{ NOTIFICATIONS : receives_logically
```

## Notes

- `USER_ROLES` enforces many-to-many between `users` and `roles` (auth-service DB).
- `registrations` enforces one active registration per `(event_id, participant_id)` using partial unique index where `status = 'REGISTERED'`.
- `events.organizer_ids` stores multi-organizer assignments as denormalized text (event-service DB).
- `event_id`, `participant_id`, and `user_id` in non-auth services are logical references to `auth-service`/`event-service` entities.
