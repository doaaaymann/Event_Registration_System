package com.event.eventservice.controller;

import com.event.eventservice.dto.request.CreateEventRequest;
import com.event.eventservice.dto.request.RescheduleEventRequest;
import com.event.eventservice.dto.request.UpdateEventRequest;
import com.event.eventservice.dto.response.CreateEventResponse;
import com.event.eventservice.dto.response.EventAvailabilityResponse;
import com.event.eventservice.dto.response.EventResponse;
import com.event.eventservice.dto.response.EventSummaryResponse;
import com.event.eventservice.entity.EventStatus;
import com.event.eventservice.security.AuthUserPrincipal;
import com.event.eventservice.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void createEventReturnsCreatedResponse() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        CreateEventRequest request = new CreateEventRequest();
        CreateEventResponse response = new CreateEventResponse(10L, "Spring Boot Workshop", EventStatus.SCHEDULED, 100);

        when(eventService.createEvent(principal, request)).thenReturn(response);

        var result = eventController.createEvent(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).createEvent(principal, request);
    }

    @Test
    void getAllEventsReturnsOkResponse() {
        List<EventSummaryResponse> response = List.of(
                new EventSummaryResponse(10L, "Workshop", "Desc", "Hall A", "2026-05-01T10:00", "2026-05-01T13:00",
                        100, 0, 100, EventStatus.SCHEDULED, 2L, List.of(2L))
        );

        when(eventService.getAllEvents()).thenReturn(response);

        var result = eventController.getAllEvents();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactlyElementsOf(response);
        verify(eventService).getAllEvents();
    }

    @Test
    void getEventReturnsOkResponse() {
        EventResponse response = new EventResponse(10L, "Workshop", "Desc", "Hall A", "2026-05-01T10:00",
                "2026-05-01T13:00", 100, 0, 100, EventStatus.SCHEDULED, 2L, List.of(2L));

        when(eventService.getEvent(10L)).thenReturn(response);

        var result = eventController.getEvent(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).getEvent(10L);
    }

    @Test
    void getOrganizerEventsReturnsOkResponse() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        List<EventSummaryResponse> response = List.of(
                new EventSummaryResponse(10L, "Workshop", "Desc", "Hall A", "2026-05-01T10:00", "2026-05-01T13:00",
                        100, 0, 100, EventStatus.SCHEDULED, 2L, List.of(2L))
        );

        when(eventService.getOrganizerEvents(principal, 2L)).thenReturn(response);

        var result = eventController.getOrganizerEvents(principal, 2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactlyElementsOf(response);
        verify(eventService).getOrganizerEvents(principal, 2L);
    }

    @Test
    void updateEventReturnsOkResponse() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        UpdateEventRequest request = new UpdateEventRequest();
        EventResponse response = new EventResponse(10L, "Workshop", "Desc", "Hall B", "2026-05-01T11:00",
                "2026-05-01T14:00", 120, 0, 120, EventStatus.SCHEDULED, 2L, List.of(2L));

        when(eventService.updateEvent(principal, 10L, request)).thenReturn(response);

        var result = eventController.updateEvent(principal, 10L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).updateEvent(principal, 10L, request);
    }

    @Test
    void cancelEventReturnsOkResponse() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        EventResponse response = new EventResponse(10L, "Workshop", "Desc", "Hall A", "2026-05-01T10:00",
                "2026-05-01T13:00", 100, 0, 100, EventStatus.CANCELLED, 2L, List.of(2L));

        when(eventService.cancelEvent(principal, 10L)).thenReturn(response);

        var result = eventController.cancelEvent(principal, 10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).cancelEvent(principal, 10L);
    }

    @Test
    void rescheduleEventReturnsOkResponse() {
        AuthUserPrincipal principal = new AuthUserPrincipal(2L, "organizer@example.com", List.of("ORGANIZER"));
        RescheduleEventRequest request = new RescheduleEventRequest();
        EventResponse response = new EventResponse(10L, "Workshop", "Desc", "Hall A", "2026-05-02T10:00",
                "2026-05-02T13:00", 100, 0, 100, EventStatus.RESCHEDULED, 2L, List.of(2L));

        when(eventService.rescheduleEvent(principal, 10L, request)).thenReturn(response);

        var result = eventController.rescheduleEvent(principal, 10L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).rescheduleEvent(principal, 10L, request);
    }

    @Test
    void getAvailabilityReturnsOkResponse() {
        EventAvailabilityResponse response = new EventAvailabilityResponse(10L, EventStatus.SCHEDULED, 100, 0, 100, true);

        when(eventService.getAvailability(10L)).thenReturn(response);

        var result = eventController.getAvailability(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(eventService).getAvailability(10L);
    }
}
