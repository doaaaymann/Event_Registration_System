package com.event.eventservice.client;

import com.event.eventservice.dto.client.RegistrationSummaryResponse;
import com.event.eventservice.dto.response.RegistrationCountResponse;
import com.event.eventservice.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RegistrationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceClient.class);

    private final RestClient restClient;

    public RegistrationServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://registration-service").build();
    }

    public int getRegisteredCount(Long eventId) {
        try {
            RegistrationCountResponse response = restClient.get()
                    .uri("/api/registrations/events/{eventId}/count", eventId)
                    .retrieve()
                    .body(RegistrationCountResponse.class);
            if (response == null || response.getRegisteredCount() == null) {
                throw new DownstreamServiceException("Registration count is unavailable for event " + eventId);
            }
            return Math.max(response.getRegisteredCount(), 0);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw unavailableCount(eventId, ex);
        } catch (RestClientException ex) {
            throw unavailableCount(eventId, ex);
        }
    }

    public Map<Long, Integer> getRegisteredCounts(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<RegistrationCountResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/registrations/events/counts")
                            .queryParam("eventIds", eventIds.toArray())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new DownstreamServiceException("Registration counts are unavailable");
            }
            return response.stream()
                    .collect(Collectors.toMap(
                            RegistrationCountResponse::getEventId,
                            count -> Math.max(count.getRegisteredCount(), 0),
                            (left, right) -> right));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw unavailableCounts(ex);
        } catch (RestClientException ex) {
            throw unavailableCounts(ex);
        }
    }

    public List<RegistrationSummaryResponse> getEventRegistrationsOrEmpty(Long eventId) {
        try {
            List<RegistrationSummaryResponse> response = restClient.get()
                    .uri("/api/registrations/events/{eventId}", eventId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? List.of() : response;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("No registration-service instances available for event registrations {}: {}", eventId, ex.getMessage());
            return List.of();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch registrations for event {}: {}", eventId, ex.getMessage());
            return List.of();
        }
    }

    private DownstreamServiceException unavailableCount(Long eventId, Exception ex) {
        log.warn("Failed to fetch registration count for event {}: {}", eventId, ex.getMessage());
        return new DownstreamServiceException("Registration count is unavailable for event " + eventId);
    }

    private DownstreamServiceException unavailableCounts(Exception ex) {
        log.warn("Failed to fetch registration counts: {}", ex.getMessage());
        return new DownstreamServiceException("Registration counts are unavailable");
    }
}
