package com.event.eventservice.dto.response;

import com.event.eventservice.entity.EventStatus;

public class EventAvailabilityResponse {

    private final Long eventId;
    private final EventStatus status;
    private final Integer maxSeats;
    private final Integer registeredCount;
    private final Integer availableSeats;
    private final boolean registrationOpen;

    public EventAvailabilityResponse(Long eventId,
                                     EventStatus status,
                                     Integer maxSeats,
                                     Integer registeredCount,
                                     Integer availableSeats,
                                     boolean registrationOpen) {
        this.eventId = eventId;
        this.status = status;
        this.maxSeats = maxSeats;
        this.registeredCount = registeredCount;
        this.availableSeats = availableSeats;
        this.registrationOpen = registrationOpen;
    }

    public Long getEventId() {
        return eventId;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }
}
