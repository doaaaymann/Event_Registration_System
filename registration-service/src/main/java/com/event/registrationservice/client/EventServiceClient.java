package com.event.registrationservice.client;

import com.event.registrationservice.dto.client.EventAvailabilityResponse;
import com.event.registrationservice.dto.client.EventDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    @GetMapping("/api/events/{eventId}")
    EventDetailsResponse getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/api/events/{eventId}/availability")
    EventAvailabilityResponse getAvailability(@PathVariable("eventId") Long eventId);
}
