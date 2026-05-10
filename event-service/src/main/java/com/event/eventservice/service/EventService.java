package com.event.eventservice.service;

import com.event.eventservice.client.NotificationServiceClient;
import com.event.eventservice.client.RegistrationServiceClient;
import com.event.eventservice.dto.client.NotificationTriggerRequest;
import com.event.eventservice.dto.client.RegistrationSummaryResponse;
import com.event.eventservice.dto.request.AssignOrganizerRequest;
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
import com.event.eventservice.ocl.EventOcl;
import com.event.eventservice.repository.EventRepository;
import com.event.eventservice.security.AuthUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final RegistrationServiceClient registrationServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public EventService(EventRepository eventRepository,
                        RegistrationServiceClient registrationServiceClient,
                        NotificationServiceClient notificationServiceClient) {
        this.eventRepository = eventRepository;
        this.registrationServiceClient = registrationServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    @Transactional
    public CreateEventResponse createEvent(AuthUserPrincipal principal, CreateEventRequest request) {
        EventOcl.requireOrganizerOrAdmin(principal);
        List<Long> organizerIds = normalizeOrganizerIds(request.getOrganizerIds());
        EventOcl.requirePrincipalOwnsOrganizerScope(principal, organizerIds);
        EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

        Event event = new Event();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation().trim());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setMaxSeats(request.getMaxSeats());
        event.setOrganizerIds(organizerIds);
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
        List<Event> events = eventRepository.findAllByOrderByStartTimeAsc();
        Map<Long, Integer> registeredCounts = getRegisteredCounts(events);
        return events.stream()
                .map(event -> toSummaryResponse(event, registeredCounts.getOrDefault(event.getId(), 0)))
                .toList();
    }

    public List<EventSummaryResponse> getOrganizerEvents(AuthUserPrincipal principal, Long organizerId) {
        ensureOrganizerOwnerOrAdmin(principal, List.of(organizerId));
        List<Event> events = eventRepository.findAllByOrderByStartTimeAsc().stream()
                .filter(event -> event.getOrganizerIds().contains(organizerId))
                .toList();
        Map<Long, Integer> registeredCounts = getRegisteredCounts(events);
        return events.stream()
                .map(event -> toSummaryResponse(event, registeredCounts.getOrDefault(event.getId(), 0)))
                .toList();
    }

    public EventResponse getEvent(Long eventId) {
        return toEventResponse(getEventEntity(eventId));
    }

    @Transactional
    public EventResponse updateEvent(AuthUserPrincipal principal, Long eventId, UpdateEventRequest request) {
        Event event = getEventEntity(eventId);
        EventOcl.requireEventOwnerOrAdmin(principal, event);
        EventOcl.requireNotCancelled(event);
        EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

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
        EventOcl.requireEventOwnerOrAdmin(principal, event);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        Event savedEvent = eventRepository.save(event);
        sendEventCancelledNotifications(savedEvent, principal);
        return toEventResponse(savedEvent);
    }

    @Transactional
    public EventResponse rescheduleEvent(AuthUserPrincipal principal, Long eventId, RescheduleEventRequest request) {
        Event event = getEventEntity(eventId);
        EventOcl.requireEventOwnerOrAdmin(principal, event);
        EventOcl.requireNotCancelled(event);
        EventOcl.requireEndAfterStart(request.getStartTime(), request.getEndTime());

        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setStatus(EventStatus.RESCHEDULED);

        Event savedEvent = eventRepository.save(event);
        sendEventRescheduledNotifications(savedEvent, principal);
        return toEventResponse(savedEvent);
    }

    @Transactional
    public EventResponse assignOrganizer(AuthUserPrincipal principal, Long eventId, AssignOrganizerRequest request) {
        EventOcl.requireAdmin(principal);
        Event event = getEventEntity(eventId);
        EventOcl.requireNotCancelled(event);
        event.setOrganizerIds(normalizeOrganizerIds(request.getOrganizerIds()));
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

    public void ensureOrganizerOwnerOrAdmin(AuthUserPrincipal principal, List<Long> organizerIds) {
        EventOcl.requireOrganizerOrAdmin(principal);
        EventOcl.requirePrincipalOwnsOrganizerScope(principal, organizerIds);
    }

    private void ensureEventOwnerOrAdmin(AuthUserPrincipal principal, Event event) {
        EventOcl.requireEventOwnerOrAdmin(principal, event);
    }

    private void ensureAdmin(AuthUserPrincipal principal) {
        EventOcl.requireAdmin(principal);
    }

    private void ensureOrganizerOrAdmin(AuthUserPrincipal principal) {
        EventOcl.requireOrganizerOrAdmin(principal);
    }

    private void ensurePrincipalOwnsOrganizerScope(AuthUserPrincipal principal, List<Long> organizerIds) {
        EventOcl.requirePrincipalOwnsOrganizerScope(principal, organizerIds);
    }

    private void ensureNotCancelled(Event event) {
        EventOcl.requireNotCancelled(event);
    }

    private void validateSchedule(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        EventOcl.requireEndAfterStart(startTime, endTime);
    }

    private Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
    }

    private EventSummaryResponse toSummaryResponse(Event event, int registeredCount) {
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
                event.getOrganizerId(),
                event.getOrganizerIds()
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
                event.getOrganizerId(),
                event.getOrganizerIds()
        );
    }

    private int getRegisteredCount(Long eventId) {
        return registrationServiceClient.getRegisteredCount(eventId);
    }

    private Map<Long, Integer> getRegisteredCounts(List<Event> events) {
        return registrationServiceClient.getRegisteredCounts(
                events.stream().map(Event::getId).toList()
        );
    }

    private int calculateAvailableSeats(Integer maxSeats, int registeredCount) {
        return Math.max(maxSeats - registeredCount, 0);
    }

    private void sendEventCancelledNotifications(Event event, AuthUserPrincipal principal) {
        List<Long> recipientIds = getAffectedUserIds(event, principal);
        if (recipientIds.isEmpty()) {
            return;
        }
        notificationServiceClient.sendEventCancelled(new NotificationTriggerRequest(
                recipientIds,
                "EVENT_CANCELLED",
                "Event Cancelled",
                event.getTitle() + " was cancelled"
        ));
    }

    private void sendEventRescheduledNotifications(Event event, AuthUserPrincipal principal) {
        List<Long> recipientIds = getAffectedUserIds(event, principal);
        if (recipientIds.isEmpty()) {
            return;
        }
        notificationServiceClient.sendEventRescheduled(new NotificationTriggerRequest(
                recipientIds,
                "EVENT_RESCHEDULED",
                "Event Rescheduled",
                event.getTitle() + " has been rescheduled"
        ));
    }

    private List<Long> getAffectedUserIds(Event event, AuthUserPrincipal principal) {
        LinkedHashSet<Long> recipientIds = new LinkedHashSet<>();
        if (principal != null && principal.getUserId() != null) {
            recipientIds.add(principal.getUserId());
        }
        recipientIds.addAll(event.getOrganizerIds().stream()
                .filter(organizerId -> organizerId != null && organizerId > 0)
                .toList());
        recipientIds.addAll(getActiveParticipantIds(event.getId()));
        return recipientIds.stream().toList();
    }

    private List<Long> normalizeOrganizerIds(List<Long> organizerIds) {
        List<Long> normalized = organizerIds == null ? List.of() : organizerIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BadRequestException("At least one organizer is required");
        }
        return normalized;
    }

    private List<Long> getActiveParticipantIds(Long eventId) {
        return registrationServiceClient.getEventRegistrationsOrEmpty(eventId).stream()
                .filter(registration -> "REGISTERED".equalsIgnoreCase(registration.getStatus()))
                .map(RegistrationSummaryResponse::getParticipantId)
                .filter(participantId -> participantId != null && participantId > 0)
                .distinct()
                .collect(Collectors.toList());
    }
}
