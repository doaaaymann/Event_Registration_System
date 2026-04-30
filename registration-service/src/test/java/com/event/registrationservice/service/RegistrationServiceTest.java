package com.event.registrationservice.service;

import com.event.registrationservice.client.EventServiceClient;
import com.event.registrationservice.client.NotificationServiceClient;
import com.event.registrationservice.dto.client.EventAvailabilityResponse;
import com.event.registrationservice.dto.client.EventDetailsResponse;
import com.event.registrationservice.dto.request.CreateRegistrationRequest;
import com.event.registrationservice.dto.response.RegistrationResponse;
import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.exception.BadRequestException;
import com.event.registrationservice.exception.ConflictException;
import com.event.registrationservice.exception.ForbiddenOperationException;
import com.event.registrationservice.repository.RegistrationRepository;
import com.event.registrationservice.security.AuthenticatedUser;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private EventLockManager eventLockManager;

    @InjectMocks
    private RegistrationService registrationService;

    private AuthenticatedUser participant;

    @BeforeEach
    void setUp() {
        participant = new AuthenticatedUser(1L, "participant@example.com", List.of("PARTICIPANT"));
    }

    @Test
    void createRegistrationSucceedsForParticipantWhenEventIsScheduledAndSeatsAvailable() {
        stubEventLockManager();

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);

        when(registrationRepository.existsByEventIdAndParticipantIdAndStatus(10L, 1L, RegistrationStatus.REGISTERED))
                .thenReturn(false);
        when(eventServiceClient.getEvent(10L)).thenReturn(scheduledEvent());
        when(eventServiceClient.getAvailability(10L)).thenReturn(availableSeats(4));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            registration.setId(100L);
            return registration;
        });

        RegistrationResponse response = registrationService.createRegistration(participant, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getEventId()).isEqualTo(10L);
        assertThat(response.getParticipantId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("REGISTERED");

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        verify(notificationServiceClient).sendRegistrationCreated(any());
    }

    @Test
    void createRegistrationRejectsNonParticipantUsers() {
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);
        AuthenticatedUser organizer = new AuthenticatedUser(5L, "organizer@example.com", List.of("ORGANIZER"));

        assertThatThrownBy(() -> registrationService.createRegistration(organizer, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only PARTICIPANT users can register for events");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void createRegistrationRejectsDuplicateRegistration() {
        stubEventLockManager();

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);
        when(registrationRepository.existsByEventIdAndParticipantIdAndStatus(10L, 1L, RegistrationStatus.REGISTERED))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.createRegistration(participant, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User is already registered for this event");
    }

    @Test
    void createRegistrationRejectsWhenNoSeatsAvailable() {
        stubEventLockManager();

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);

        when(registrationRepository.existsByEventIdAndParticipantIdAndStatus(10L, 1L, RegistrationStatus.REGISTERED))
                .thenReturn(false);
        when(eventServiceClient.getEvent(10L)).thenReturn(scheduledEvent());
        when(eventServiceClient.getAvailability(10L)).thenReturn(availability(0, true));

        assertThatThrownBy(() -> registrationService.createRegistration(participant, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No seats available for this event");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void createRegistrationAllowsReregistrationAfterCancellation() {
        stubEventLockManager();

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);

        when(registrationRepository.existsByEventIdAndParticipantIdAndStatus(10L, 1L, RegistrationStatus.REGISTERED))
                .thenReturn(false);
        when(eventServiceClient.getEvent(10L)).thenReturn(scheduledEvent());
        when(eventServiceClient.getAvailability(10L)).thenReturn(availableSeats(3));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            registration.setId(101L);
            return registration;
        });

        RegistrationResponse response = registrationService.createRegistration(participant, request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getStatus()).isEqualTo("REGISTERED");
    }

    @Test
    void cancelRegistrationMarksStatusCancelledAndSendsNotification() {
        Registration registration = existingRegistration();
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventServiceClient.getEvent(10L)).thenReturn(scheduledEvent());

        RegistrationResponse response = registrationService.cancelRegistration(100L, participant);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getCancelledAt()).isNotNull();
        verify(notificationServiceClient).sendRegistrationCancelled(any());
    }

    @Test
    void cancelRegistrationRejectsNonOwner() {
        Registration registration = existingRegistration();
        when(registrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        AuthenticatedUser anotherUser = new AuthenticatedUser(7L, "other@example.com", List.of("PARTICIPANT"));

        assertThatThrownBy(() -> registrationService.cancelRegistration(100L, anotherUser))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the registration owner can perform this action");
    }

    @Test
    void getEventRegistrationsAllowsOrganizerOwner() {
        EventDetailsResponse event = scheduledEvent();
        event.setOrganizerId(5L);
        when(eventServiceClient.getEvent(10L)).thenReturn(event);
        when(registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(10L)).thenReturn(List.of(existingRegistration()));

        AuthenticatedUser organizer = new AuthenticatedUser(5L, "organizer@example.com", List.of("ORGANIZER"));
        List<RegistrationResponse> result = registrationService.getEventRegistrations(10L, organizer);

        assertThat(result).hasSize(1);
    }

    @Test
    void getEventRegistrationsRejectsParticipant() {
        EventDetailsResponse event = scheduledEvent();
        event.setOrganizerId(5L);
        when(eventServiceClient.getEvent(10L)).thenReturn(event);

        assertThatThrownBy(() -> registrationService.getEventRegistrations(10L, participant))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the event organizer or ADMIN can view event participants");
    }

    private Registration existingRegistration() {
        Registration registration = new Registration();
        registration.setId(100L);
        registration.setEventId(10L);
        registration.setParticipantId(1L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.now().minusHours(2));
        return registration;
    }

    private EventDetailsResponse scheduledEvent() {
        EventDetailsResponse event = new EventDetailsResponse();
        event.setId(10L);
        event.setTitle("Spring Boot Workshop");
        event.setStatus("SCHEDULED");
        return event;
    }

    private EventAvailabilityResponse availableSeats(int seats) {
        return availability(seats, seats > 0);
    }

    private EventAvailabilityResponse availability(int seats, boolean registrationOpen) {
        EventAvailabilityResponse response = new EventAvailabilityResponse();
        response.setEventId(10L);
        response.setStatus("SCHEDULED");
        response.setAvailableSeats(seats);
        response.setRegistrationOpen(registrationOpen);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void stubEventLockManager() {
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(eventLockManager).executeWithLock(any(), any());
    }
}
