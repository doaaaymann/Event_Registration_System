package com.event.eventservice.service;

import com.event.eventservice.client.NotificationServiceClient;
import com.event.eventservice.client.RegistrationServiceClient;
import com.event.eventservice.dto.client.NotificationTriggerRequest;
import com.event.eventservice.dto.client.RegistrationSummaryResponse;
import com.event.eventservice.dto.request.CreateEventRequest;
import com.event.eventservice.dto.request.RescheduleEventRequest;
import com.event.eventservice.dto.request.UpdateEventRequest;
import com.event.eventservice.dto.response.CreateEventResponse;
import com.event.eventservice.dto.response.EventAvailabilityResponse;
import com.event.eventservice.dto.response.EventResponse;
import com.event.eventservice.entity.Event;
import com.event.eventservice.entity.EventStatus;
import com.event.eventservice.exception.BadRequestException;
import com.event.eventservice.exception.DownstreamServiceException;
import com.event.eventservice.exception.ResourceNotFoundException;
import com.event.eventservice.repository.EventRepository;
import com.event.eventservice.security.AuthUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationServiceClient registrationServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private EventService eventService;

    private AuthUserPrincipal organizerPrincipal;
    private AuthUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        organizerPrincipal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        adminPrincipal = new AuthUserPrincipal(1L, "admin@event.local", List.of("ADMIN"));
    }

    @Test
    void createEventCreatesScheduledEvent() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Spring Boot Workshop");
        request.setDescription("Hands-on workshop");
        request.setLocation("Cairo Hall A");
        request.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 1, 13, 0));
        request.setMaxSeats(100);
        request.setOrganizerId(2L);

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });
        CreateEventResponse response = eventService.createEvent(organizerPrincipal, request);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getTitle()).isEqualTo("Spring Boot Workshop");
        assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.SCHEDULED);
        assertThat(savedEvent.getOrganizerId()).isEqualTo(2L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(EventStatus.SCHEDULED);
        assertThat(response.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    void createEventRejectsOrganizerCreatingForDifferentOrganizerId() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Spring Boot Workshop");
        request.setLocation("Cairo Hall A");
        request.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 1, 13, 0));
        request.setMaxSeats(100);
        request.setOrganizerId(99L);

        assertThatThrownBy(() -> eventService.createEvent(organizerPrincipal, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access is denied");
    }

    @Test
    void adminCanCreateEventForAnyOrganizer() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Spring Boot Workshop");
        request.setLocation("Cairo Hall A");
        request.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 1, 13, 0));
        request.setMaxSeats(100);
        request.setOrganizerId(5L);

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });
        CreateEventResponse response = eventService.createEvent(adminPrincipal, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAvailableSeats()).isEqualTo(100);
    }

    @Test
    void getEventReturnsDetails() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event(10L, 2L, EventStatus.SCHEDULED)));
        when(registrationServiceClient.getRegisteredCount(10L)).thenReturn(35);

        EventResponse response = eventService.getEvent(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Spring Boot Workshop");
        assertThat(response.getRegisteredCount()).isEqualTo(35);
        assertThat(response.getAvailableSeats()).isEqualTo(65);
    }

    @Test
    void updateEventAllowsOwner() {
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated Workshop");
        request.setDescription("Updated description");
        request.setLocation("Hall B");
        request.setStartTime(LocalDateTime.of(2026, 5, 2, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 2, 13, 0));
        request.setMaxSeats(120);

        Event event = event(10L, 2L, EventStatus.SCHEDULED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationServiceClient.getRegisteredCount(10L)).thenReturn(0);

        EventResponse response = eventService.updateEvent(organizerPrincipal, 10L, request);

        assertThat(response.getTitle()).isEqualTo("Updated Workshop");
        assertThat(response.getLocation()).isEqualTo("Hall B");
        assertThat(response.getMaxSeats()).isEqualTo(120);
    }

    @Test
    void updateEventRejectsCancelledEvent() {
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated Workshop");
        request.setLocation("Hall B");
        request.setDescription("Updated description");
        request.setStartTime(LocalDateTime.of(2026, 5, 2, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 2, 13, 0));
        request.setMaxSeats(120);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event(10L, 2L, EventStatus.CANCELLED)));

        assertThatThrownBy(() -> eventService.updateEvent(organizerPrincipal, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cancelled events cannot be modified");
    }

    @Test
    void cancelEventSetsStatusToCancelled() {
        Event event = event(10L, 2L, EventStatus.SCHEDULED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationServiceClient.getRegisteredCount(10L)).thenReturn(0);
        when(registrationServiceClient.getEventRegistrationsOrEmpty(10L)).thenReturn(List.of(
                registration(100L, 10L, 7L, "REGISTERED"),
                registration(101L, 10L, 8L, "REGISTERED"),
                registration(102L, 10L, 8L, "CANCELLED")
        ));

        EventResponse response = eventService.cancelEvent(organizerPrincipal, 10L);

        assertThat(response.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(notificationServiceClient).sendEventCancelled(argThat(request ->
                request.getUserIds().equals(List.of(7L, 8L))
                        && "EVENT_CANCELLED".equals(request.getType())
                        && "Event Cancelled".equals(request.getTitle())
        ));
    }

    @Test
    void cancelEventRejectsAlreadyCancelledEvent() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event(10L, 2L, EventStatus.CANCELLED)));

        assertThatThrownBy(() -> eventService.cancelEvent(organizerPrincipal, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Event is already cancelled");
    }

    @Test
    void rescheduleEventMarksEventAsRescheduled() {
        RescheduleEventRequest request = new RescheduleEventRequest();
        request.setStartTime(LocalDateTime.of(2026, 5, 3, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 3, 13, 0));

        Event event = event(10L, 2L, EventStatus.SCHEDULED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationServiceClient.getRegisteredCount(10L)).thenReturn(12);
        when(registrationServiceClient.getEventRegistrationsOrEmpty(10L)).thenReturn(List.of(
                registration(100L, 10L, 7L, "REGISTERED"),
                registration(101L, 10L, 9L, "REGISTERED")
        ));

        EventResponse response = eventService.rescheduleEvent(organizerPrincipal, 10L, request);

        assertThat(response.getStatus()).isEqualTo(EventStatus.RESCHEDULED);
        assertThat(response.getStartTime()).isEqualTo("2026-05-03T10:00");
        verify(notificationServiceClient).sendEventRescheduled(argThat(requestBody ->
                requestBody.getUserIds().equals(List.of(7L, 9L))
                        && "EVENT_RESCHEDULED".equals(requestBody.getType())
                        && "Event Rescheduled".equals(requestBody.getTitle())
        ));
    }

    @Test
    void getAvailabilityReflectsEventState() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event(10L, 2L, EventStatus.SCHEDULED)));
        when(registrationServiceClient.getRegisteredCount(10L)).thenReturn(40);

        EventAvailabilityResponse response = eventService.getAvailability(10L);

        assertThat(response.getEventId()).isEqualTo(10L);
        assertThat(response.getRegisteredCount()).isEqualTo(40);
        assertThat(response.getAvailableSeats()).isEqualTo(60);
        assertThat(response.isRegistrationOpen()).isTrue();
    }

    @Test
    void getOrganizerEventsRejectsDifferentOrganizer() {
        assertThatThrownBy(() -> eventService.getOrganizerEvents(organizerPrincipal, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access is denied");
    }

    @Test
    void getAllEventsUsesBatchRegistrationCounts() {
        when(eventRepository.findAllByOrderByStartTimeAsc()).thenReturn(List.of(
                event(10L, 2L, EventStatus.SCHEDULED),
                event(11L, 2L, EventStatus.SCHEDULED)
        ));
        when(registrationServiceClient.getRegisteredCounts(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 35, 11L, 10));

        var response = eventService.getAllEvents();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getRegisteredCount()).isEqualTo(35);
        assertThat(response.get(1).getRegisteredCount()).isEqualTo(10);
    }

    @Test
    void getAvailabilityFailsClosedWhenRegistrationCountIsUnavailable() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event(10L, 2L, EventStatus.SCHEDULED)));
        when(registrationServiceClient.getRegisteredCount(10L))
                .thenThrow(new DownstreamServiceException("Registration count is unavailable for event 10"));

        assertThatThrownBy(() -> eventService.getAvailability(10L))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessage("Registration count is unavailable for event 10");
    }

    @Test
    void getEventThrowsWhenMissing() {
        when(eventRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found with id 10");
    }

    private static Event event(Long id, Long organizerId, EventStatus status) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Spring Boot Workshop");
        event.setDescription("Hands-on workshop");
        event.setLocation("Cairo Hall A");
        event.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        event.setEndTime(LocalDateTime.of(2026, 5, 1, 13, 0));
        event.setMaxSeats(100);
        event.setOrganizerId(organizerId);
        event.setStatus(status);
        return event;
    }

    private static RegistrationSummaryResponse registration(Long id, Long eventId, Long participantId, String status) {
        RegistrationSummaryResponse response = new RegistrationSummaryResponse();
        response.setId(id);
        response.setEventId(eventId);
        response.setParticipantId(participantId);
        response.setStatus(status);
        return response;
    }
}
