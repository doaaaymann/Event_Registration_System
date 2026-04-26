package com.event.eventservice.dto.client;

import java.util.List;

public class NotificationTriggerRequest {

    private final List<Long> userIds;
    private final String type;
    private final String title;
    private final String message;

    public NotificationTriggerRequest(List<Long> userIds, String type, String title, String message) {
        this.userIds = userIds;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
