package com.event.registrationservice.dto.response;

public class RegistrationCountResponse {

    private final Long eventId;
    private final Long registeredCount;

    public RegistrationCountResponse(Long eventId, Long registeredCount) {
        this.eventId = eventId;
        this.registeredCount = registeredCount;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getRegisteredCount() {
        return registeredCount;
    }
}
