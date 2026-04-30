package com.event.registrationservice.dto.request;

import jakarta.validation.constraints.NotNull;

public class CreateRegistrationRequest {

    @NotNull(message = "eventId is required")
    private Long eventId;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
