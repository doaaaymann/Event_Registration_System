package com.event.eventservice.dto.response;

import com.event.eventservice.entity.EventStatus;

public class CreateEventResponse {

    private final Long id;
    private final String title;
    private final EventStatus status;
    private final Integer availableSeats;

    public CreateEventResponse(Long id, String title, EventStatus status, Integer availableSeats) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.availableSeats = availableSeats;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }
}
