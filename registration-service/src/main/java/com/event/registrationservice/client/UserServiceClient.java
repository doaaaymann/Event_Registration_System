package com.event.registrationservice.client;

import com.event.registrationservice.dto.client.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface UserServiceClient {

    @GetMapping("/api/auth/internal/users/{userId}")
    UserDetailsResponse getUserById(@PathVariable("userId") Long userId);
}
