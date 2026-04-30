package com.event.notificationservice.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class InternalNotificationTriggerRequest {

    private Long userId;
    private List<Long> userIds;

    @NotBlank
    private String type;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
