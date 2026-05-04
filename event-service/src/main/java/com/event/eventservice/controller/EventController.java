package com.event.eventservice.controller;

import com.event.eventservice.dto.request.AssignOrganizerRequest;
import com.event.eventservice.dto.request.CreateEventRequest;
import com.event.eventservice.dto.request.RescheduleEventRequest;
import com.event.eventservice.dto.request.UpdateEventRequest;
import com.event.eventservice.dto.response.CreateEventResponse;
import com.event.eventservice.dto.response.EventAvailabilityResponse;
import com.event.eventservice.dto.response.EventResponse;
import com.event.eventservice.dto.response.EventSummaryResponse;
import com.event.eventservice.security.AuthUserPrincipal;
import com.event.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<CreateEventResponse> createEvent(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                           @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(principal, request));
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping("/organizers/{organizerId}")
    public ResponseEntity<List<EventSummaryResponse>> getOrganizerEvents(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(principal, organizerId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                     @PathVariable Long eventId,
                                                     @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(principal, eventId, request));
    }

    @PatchMapping("/{eventId}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                     @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.cancelEvent(principal, eventId));
    }

    @PatchMapping("/{eventId}/reschedule")
    public ResponseEntity<EventResponse> rescheduleEvent(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                         @PathVariable Long eventId,
                                                         @Valid @RequestBody RescheduleEventRequest request) {
        return ResponseEntity.ok(eventService.rescheduleEvent(principal, eventId, request));
    }

    @PatchMapping("/{eventId}/organizer")
    public ResponseEntity<EventResponse> assignOrganizer(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                         @PathVariable Long eventId,
                                                         @Valid @RequestBody AssignOrganizerRequest request) {
        return ResponseEntity.ok(eventService.assignOrganizer(principal, eventId, request));
    }

    @GetMapping("/{eventId}/availability")
    public ResponseEntity<EventAvailabilityResponse> getAvailability(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getAvailability(eventId));
    }
}
