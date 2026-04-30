package com.event.notificationservice.controller;

import com.event.notificationservice.dto.request.CreateNotificationRequest;
import com.event.notificationservice.dto.request.InternalNotificationTriggerRequest;
import com.event.notificationservice.dto.response.NotificationResponse;
import com.event.notificationservice.security.AuthenticatedUser;
import com.event.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(@Valid @RequestBody CreateNotificationRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return notificationService.createNotification(authenticatedUser, request);
    }

    @GetMapping("/users/{userId}")
    public List<NotificationResponse> getNotificationsByUserId(@PathVariable Long userId,
                                                               @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return notificationService.getNotificationsByUserId(authenticatedUser, userId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId,
                                           @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return notificationService.markAsRead(authenticatedUser, notificationId);
    }

    @PostMapping("/internal/registration-created")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> handleRegistrationCreated(@RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
                                                                @Valid @RequestBody InternalNotificationTriggerRequest request) {
        return notificationService.handleInternalTrigger(internalApiKey, request);
    }

    @PostMapping("/internal/registration-cancelled")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> handleRegistrationCancelled(@RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
                                                                  @Valid @RequestBody InternalNotificationTriggerRequest request) {
        return notificationService.handleInternalTrigger(internalApiKey, request);
    }

    @PostMapping("/internal/event-cancelled")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> handleEventCancelled(@RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
                                                           @Valid @RequestBody InternalNotificationTriggerRequest request) {
        return notificationService.handleInternalTrigger(internalApiKey, request);
    }

    @PostMapping("/internal/event-rescheduled")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> handleEventRescheduled(@RequestHeader(name = "X-Internal-Api-Key", required = false) String internalApiKey,
                                                             @Valid @RequestBody InternalNotificationTriggerRequest request) {
        return notificationService.handleInternalTrigger(internalApiKey, request);
    }
}
