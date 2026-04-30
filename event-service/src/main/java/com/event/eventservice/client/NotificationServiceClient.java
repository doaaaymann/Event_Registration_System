package com.event.eventservice.client;

import com.event.eventservice.dto.client.NotificationTriggerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestClient restClient;

    public NotificationServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://notification-service").build();
    }

    public void sendEventCancelled(NotificationTriggerRequest request) {
        postTrigger("/api/notifications/internal/event-cancelled", request);
    }

    public void sendEventRescheduled(NotificationTriggerRequest request) {
        postTrigger("/api/notifications/internal/event-rescheduled", request);
    }

    private void postTrigger(String path, NotificationTriggerRequest request) {
        try {
            restClient.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (IllegalStateException ex) {
            log.warn("No notification-service instances available for {}: {}", path, ex.getMessage());
        } catch (RestClientException ex) {
            log.warn("Failed to call notification-service {}: {}", path, ex.getMessage());
        }
    }
}
