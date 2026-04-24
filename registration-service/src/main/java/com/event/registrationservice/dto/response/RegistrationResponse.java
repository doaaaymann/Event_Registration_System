package com.event.registrationservice.dto.response;

import java.time.LocalDateTime;

public class RegistrationResponse {

    private final Long id;
    private final Long eventId;
    private final Long participantId;
    private final String status;
    private final LocalDateTime registeredAt;
    private final LocalDateTime cancelledAt;

    public RegistrationResponse(Long id,
                                Long eventId,
                                Long participantId,
                                String status,
                                LocalDateTime registeredAt,
                                LocalDateTime cancelledAt) {
        this.id = id;
        this.eventId = eventId;
        this.participantId = participantId;
        this.status = status;
        this.registeredAt = registeredAt;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
