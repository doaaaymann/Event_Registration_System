package com.event.notificationservice.service;

import com.event.notificationservice.dto.request.CreateNotificationRequest;
import com.event.notificationservice.dto.request.InternalNotificationTriggerRequest;
import com.event.notificationservice.dto.response.NotificationResponse;
import com.event.notificationservice.config.InternalApiProperties;
import com.event.notificationservice.entity.Notification;
import com.event.notificationservice.exception.BadRequestException;
import com.event.notificationservice.exception.ForbiddenOperationException;
import com.event.notificationservice.exception.ResourceNotFoundException;
import com.event.notificationservice.repository.NotificationRepository;
import com.event.notificationservice.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final InternalApiProperties internalApiProperties;

    public NotificationService(NotificationRepository notificationRepository,
                               InternalApiProperties internalApiProperties) {
        this.notificationRepository = notificationRepository;
        this.internalApiProperties = internalApiProperties;
    }

    @Transactional
    public NotificationResponse createNotification(AuthenticatedUser authenticatedUser, CreateNotificationRequest request) {
        requireAuthenticatedUser(authenticatedUser);
        validateCreateRequest(request);
        enforceCreatePermission(authenticatedUser, request.getUserId());
        Notification notification = buildNotification(
                request.getUserId(),
                request.getType(),
                request.getTitle(),
                request.getMessage()
        );
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(AuthenticatedUser authenticatedUser, Long userId) {
        requireAuthenticatedUser(authenticatedUser);
        enforceReadPermission(authenticatedUser, userId);
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(AuthenticatedUser authenticatedUser, Long notificationId) {
        requireAuthenticatedUser(authenticatedUser);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        enforceReadPermission(authenticatedUser, notification.getUserId());
        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public List<NotificationResponse> handleInternalTrigger(String internalApiKey,
                                                            InternalNotificationTriggerRequest request) {
        validateInternalApiKey(internalApiKey);
        validateInternalRequest(request);
        return resolveRecipientIds(request).stream()
                .map(userId -> buildNotification(userId, request.getType(), request.getTitle(), request.getMessage()))
                .map(notificationRepository::save)
                .map(this::toResponse)
                .toList();
    }

    private void requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getUserId() == null) {
            throw new ForbiddenOperationException("Authenticated user context is required");
        }
    }

    private void validateCreateRequest(CreateNotificationRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BadRequestException("userId is required");
        }
        validateTextField(request.getType(), "type");
        validateTextField(request.getTitle(), "title");
        validateTextField(request.getMessage(), "message");
    }

    private void validateInternalRequest(InternalNotificationTriggerRequest request) {
        if (request == null) {
            throw new BadRequestException("Notification trigger request is required");
        }
        if (resolveRecipientIds(request).isEmpty()) {
            throw new BadRequestException("At least one recipient userId is required");
        }
        validateTextField(request.getType(), "type");
        validateTextField(request.getTitle(), "title");
        validateTextField(request.getMessage(), "message");
    }

    private void validateInternalApiKey(String internalApiKey) {
        if (internalApiProperties.getKey() == null || internalApiProperties.getKey().isBlank()) {
            throw new ForbiddenOperationException("Internal notification API key is not configured");
        }
        if (internalApiKey == null || !internalApiProperties.getKey().equals(internalApiKey)) {
            throw new ForbiddenOperationException("Invalid internal notification API key");
        }
    }

    private void enforceCreatePermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.getUserId().equals(targetUserId)) {
            return;
        }
        throw new ForbiddenOperationException("Only ADMIN can create notifications for other users");
    }

    private void enforceReadPermission(AuthenticatedUser authenticatedUser, Long targetUserId) {
        if (authenticatedUser.hasRole("ADMIN")) {
            return;
        }
        if (authenticatedUser.getUserId().equals(targetUserId)) {
            return;
        }
        throw new ForbiddenOperationException("Only the notification owner or ADMIN can perform this action");
    }

    private Set<Long> resolveRecipientIds(InternalNotificationTriggerRequest request) {
        Set<Long> recipientIds = new LinkedHashSet<>();
        if (request.getUserId() != null) {
            recipientIds.add(request.getUserId());
        }
        if (request.getUserIds() != null) {
            request.getUserIds().stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(recipientIds::add);
        }
        return recipientIds;
    }

    private Notification buildNotification(Long userId, String type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type.trim());
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private void validateTextField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
    }
}
