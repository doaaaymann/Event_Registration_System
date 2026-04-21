package com.event.eventservice.service;

import com.event.eventservice.client.RegistrationServiceClient;
import com.event.eventservice.dto.request.CreateEventRequest;
import com.event.eventservice.dto.request.RescheduleEventRequest;
import com.event.eventservice.dto.request.UpdateEventRequest;
import com.event.eventservice.dto.response.CreateEventResponse;
import com.event.eventservice.dto.response.EventAvailabilityResponse;
import com.event.eventservice.dto.response.EventResponse;
import com.event.eventservice.dto.response.EventSummaryResponse;
import com.event.eventservice.entity.Event;
import com.event.eventservice.entity.EventStatus;
import com.event.eventservice.exception.BadRequestException;
import com.event.eventservice.exception.ResourceNotFoundException;
import com.event.eventservice.repository.EventRepository;
import com.event.eventservice.security.AuthUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final RegistrationServiceClient registrationServiceClient;

    public EventService(EventRepository eventRepository, RegistrationServiceClient registrationServiceClient) {
        this.eventRepository = eventRepository;
        this.registrationServiceClient = registrationServiceClient;
    }

    @Transactional
    public CreateEventResponse createEvent(AuthUserPrincipal principal, CreateEventRequest request) {
        ensureOrganizerOrAdmin(principal);
        ensurePrincipalOwnsOrganizerScope(principal, request.getOrganizerId());
        validateSchedule(request.getStartTime(), request.getEndTime());

        Event event = new Event();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation().trim());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setMaxSeats(request.getMaxSeats());
        event.setOrganizerId(request.getOrganizerId());
        event.setStatus(EventStatus.SCHEDULED);

        Event savedEvent = eventRepository.save(event);
        return new CreateEventResponse(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getStatus(),
                savedEvent.getMaxSeats()
        );
    }

    public List<EventSummaryResponse> getAllEvents() {
        return eventRepository.findAllByOrderByStartTimeAsc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<EventSummaryResponse> getOrganizerEvents(AuthUserPrincipal principal, Long organizerId) {
        ensureOrganizerOwnerOrAdmin(principal, organizerId);
        return eventRepository.findByOrganizerIdOrderByStartTimeAsc(organizerId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public EventResponse getEvent(Long eventId) {
        return toEventResponse(getEventEntity(eventId));
    }

    @Transactional
    public EventResponse updateEvent(AuthUserPrincipal principal, Long eventId, UpdateEventRequest request) {
        Event event = getEventEntity(eventId);
        ensureEventOwnerOrAdmin(principal, event);
        ensureNotCancelled(event);
        validateSchedule(request.getStartTime(), request.getEndTime());

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation().trim());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setMaxSeats(request.getMaxSeats());

        return toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse cancelEvent(AuthUserPrincipal principal, Long eventId) {
        Event event = getEventEntity(eventId);
        ensureEventOwnerOrAdmin(principal, event);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        return toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse rescheduleEvent(AuthUserPrincipal principal, Long eventId, RescheduleEventRequest request) {
        Event event = getEventEntity(eventId);
        ensureEventOwnerOrAdmin(principal, event);
        ensureNotCancelled(event);
        validateSchedule(request.getStartTime(), request.getEndTime());

        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setStatus(EventStatus.RESCHEDULED);

        return toEventResponse(eventRepository.save(event));
    }

    public EventAvailabilityResponse getAvailability(Long eventId) {
        Event event = getEventEntity(eventId);
        int registeredCount = getRegisteredCount(event.getId());
        int availableSeats = calculateAvailableSeats(event.getMaxSeats(), registeredCount);
        boolean registrationOpen = event.getStatus() != EventStatus.CANCELLED && availableSeats > 0;
        return new EventAvailabilityResponse(
                event.getId(),
                event.getStatus(),
                event.getMaxSeats(),
                registeredCount,
                availableSeats,
                registrationOpen
        );
    }

    public void ensureOrganizerOwnerOrAdmin(AuthUserPrincipal principal, Long organizerId) {
        ensureOrganizerOrAdmin(principal);
        ensurePrincipalOwnsOrganizerScope(principal, organizerId);
    }

    private void ensureEventOwnerOrAdmin(AuthUserPrincipal principal, Event event) {
        ensureOrganizerOwnerOrAdmin(principal, event.getOrganizerId());
    }

    private void ensureOrganizerOrAdmin(AuthUserPrincipal principal) {
        if (principal == null || principal.getRoles().stream().noneMatch(role ->
                "ADMIN".equals(role) || "ORGANIZER".equals(role))) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private void ensurePrincipalOwnsOrganizerScope(AuthUserPrincipal principal, Long organizerId) {
        if (!principal.getRoles().contains("ADMIN") && !principal.getUserId().equals(organizerId)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private void ensureNotCancelled(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cancelled events cannot be modified");
        }
    }

    private void validateSchedule(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
    }

    private EventSummaryResponse toSummaryResponse(Event event) {
        int registeredCount = getRegisteredCount(event.getId());
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime().toString(),
                event.getEndTime().toString(),
                event.getMaxSeats(),
                registeredCount,
                calculateAvailableSeats(event.getMaxSeats(), registeredCount),
                event.getStatus(),
                event.getOrganizerId()
        );
    }

    private EventResponse toEventResponse(Event event) {
        int registeredCount = getRegisteredCount(event.getId());
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime().toString(),
                event.getEndTime().toString(),
                event.getMaxSeats(),
                registeredCount,
                calculateAvailableSeats(event.getMaxSeats(), registeredCount),
                event.getStatus(),
                event.getOrganizerId()
        );
    }

    private int getRegisteredCount(Long eventId) {
        return registrationServiceClient.getRegisteredCount(eventId);
    }

    private int calculateAvailableSeats(Integer maxSeats, int registeredCount) {
        return Math.max(maxSeats - registeredCount, 0);
    }
}
