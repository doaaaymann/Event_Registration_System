package com.event.registrationservice.client;

import com.event.registrationservice.dto.client.NotificationCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/internal/registration-created")
    void sendRegistrationCreated(@RequestBody NotificationCommand command);

    @PostMapping("/api/notifications/internal/registration-cancelled")
    void sendRegistrationCancelled(@RequestBody NotificationCommand command);
}
