package com.event.registrationservice.dto.response;

public class RegistrationCountResponse {

    private final Long eventId;
    private final long registeredCount;

    public RegistrationCountResponse(Long eventId, long registeredCount) {
        this.eventId = eventId;
        this.registeredCount = registeredCount;
    }

    public Long getEventId() {
        return eventId;
    }

    public long getRegisteredCount() {
        return registeredCount;
    }
}
