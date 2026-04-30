package com.event.notificationservice.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
}
