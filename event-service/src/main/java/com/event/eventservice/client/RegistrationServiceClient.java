package com.event.eventservice.client;

import com.event.eventservice.dto.response.RegistrationCountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
                return 0;
            }
            return Math.max(response.getRegisteredCount(), 0);
        } catch (IllegalStateException ex) {
            log.warn("No registration-service instances available for event {}: {}", eventId, ex.getMessage());
            return 0;
        } catch (RestClientException ex) {
            log.warn("Failed to fetch registration count for event {}: {}", eventId, ex.getMessage());
            return 0;
        }
    }
}
