package com.event.registrationservice.controller;

import com.event.registrationservice.dto.response.RegistrationCountResponse;
import com.event.registrationservice.service.RegistrationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationQueryController {

    private final RegistrationQueryService registrationQueryService;

    public RegistrationQueryController(RegistrationQueryService registrationQueryService) {
        this.registrationQueryService = registrationQueryService;
    }

    @GetMapping("/events/{eventId}/count")
    public ResponseEntity<RegistrationCountResponse> getRegistrationCount(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationQueryService.getRegistrationCount(eventId));
    }
}
