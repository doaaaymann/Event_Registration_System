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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private InternalApiProperties internalApiProperties;

    @InjectMocks
    private NotificationService notificationService;

    private AuthenticatedUser participant;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        participant = new AuthenticatedUser(1L, "participant@example.com", List.of("PARTICIPANT"));
        admin = new AuthenticatedUser(99L, "admin@example.com", List.of("ADMIN"));
        lenient().when(internalApiProperties.getKey()).thenReturn("test-internal-key");
    }

    @Test
    void createNotificationAllowsOwnerToCreateForSelf() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(1L);
        request.setType("REGISTRATION_CONFIRMED");
        request.setTitle("Registration Confirmed");
        request.setMessage("You are registered for Spring Boot Workshop");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(501L);
            return notification;
        });

        NotificationResponse response = notificationService.createNotification(participant, request);

        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.read()).isFalse();
    }

    @Test
    void createNotificationRejectsCrossUserCreationForNonAdmin() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(2L);
        request.setType("REGISTRATION_CONFIRMED");
        request.setTitle("Registration Confirmed");
        request.setMessage("You are registered");

        assertThatThrownBy(() -> notificationService.createNotification(participant, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only ADMIN can create notifications for other users");
    }

    @Test
    void getNotificationsByUserIdRejectsDifferentUserForParticipant() {
        assertThatThrownBy(() -> notificationService.getNotificationsByUserId(participant, 2L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the notification owner or ADMIN can perform this action");
    }

    @Test
    void markAsReadUpdatesUnreadNotification() {
        Notification notification = new Notification();
        notification.setId(501L);
        notification.setUserId(1L);
        notification.setType("REGISTRATION_CONFIRMED");
        notification.setTitle("Registration Confirmed");
        notification.setMessage("You are registered");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findById(501L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(participant, 501L);

        assertThat(response.read()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadThrowsWhenNotificationMissing() {
        when(notificationRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(participant, 888L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void handleInternalTriggerCreatesOneNotificationPerRecipient() {
        InternalNotificationTriggerRequest request = new InternalNotificationTriggerRequest();
        request.setUserId(1L);
        request.setUserIds(List.of(1L, 2L, 3L));
        request.setType("EVENT_CANCELLED");
        request.setTitle("Event Cancelled");
        request.setMessage("Spring Boot Workshop was cancelled");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(notification.getUserId() + 500L);
            return notification;
        });

        List<NotificationResponse> responses = notificationService.handleInternalTrigger("test-internal-key", request);

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(NotificationResponse::userId).containsExactly(1L, 2L, 3L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getType).containsOnly("EVENT_CANCELLED");
    }

    @Test
    void handleInternalTriggerRejectsEmptyRecipients() {
        InternalNotificationTriggerRequest request = new InternalNotificationTriggerRequest();
        request.setType("EVENT_CANCELLED");
        request.setTitle("Event Cancelled");
        request.setMessage("Spring Boot Workshop was cancelled");

        assertThatThrownBy(() -> notificationService.handleInternalTrigger("test-internal-key", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one recipient userId is required");
    }

    @Test
    void handleInternalTriggerRejectsInvalidApiKey() {
        InternalNotificationTriggerRequest request = new InternalNotificationTriggerRequest();
        request.setUserId(1L);
        request.setType("EVENT_CANCELLED");
        request.setTitle("Event Cancelled");
        request.setMessage("Spring Boot Workshop was cancelled");

        assertThatThrownBy(() -> notificationService.handleInternalTrigger("wrong-key", request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Invalid internal notification API key");
    }
}
