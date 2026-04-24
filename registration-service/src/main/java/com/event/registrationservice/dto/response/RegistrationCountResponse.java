package com.event.registrationservice.dto.response;

public class RegistrationCountResponse {

    private final Long eventId;
<<<<<<< HEAD
    private final Long registeredCount;

    public RegistrationCountResponse(Long eventId, Long registeredCount) {
=======
    private final long registeredCount;

    public RegistrationCountResponse(Long eventId, long registeredCount) {
>>>>>>> origin/Registration
        this.eventId = eventId;
        this.registeredCount = registeredCount;
    }

    public Long getEventId() {
        return eventId;
    }

<<<<<<< HEAD
    public Long getRegisteredCount() {
=======
    public long getRegisteredCount() {
>>>>>>> origin/Registration
        return registeredCount;
    }
}
