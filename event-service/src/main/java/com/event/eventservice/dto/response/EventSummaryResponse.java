package com.event.eventservice.dto.response;

import com.event.eventservice.entity.EventStatus;

public class EventSummaryResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String location;
    private final String startTime;
    private final String endTime;
    private final Integer maxSeats;
    private final Integer registeredCount;
    private final Integer availableSeats;
    private final EventStatus status;
    private final Long organizerId;

    public EventSummaryResponse(Long id,
                                String title,
                                String description,
                                String location,
                                String startTime,
                                String endTime,
                                Integer maxSeats,
                                Integer registeredCount,
                                Integer availableSeats,
                                EventStatus status,
                                Long organizerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxSeats = maxSeats;
        this.registeredCount = registeredCount;
        this.availableSeats = availableSeats;
        this.status = status;
        this.organizerId = organizerId;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
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

    public EventStatus getStatus() {
        return status;
    }

    public Long getOrganizerId() {
        return organizerId;
    }
}
