package com.event.registrationservice.controller;

import com.event.registrationservice.dto.request.CreateRegistrationRequest;
import com.event.registrationservice.dto.response.RegistrationResponse;
import com.event.registrationservice.security.AuthenticatedUser;
import com.event.registrationservice.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse createRegistration(@Valid @RequestBody CreateRegistrationRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return registrationService.createRegistration(authenticatedUser, request);
    }

    @GetMapping("/{registrationId}")
    public RegistrationResponse getRegistration(@PathVariable Long registrationId,
                                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return registrationService.getRegistration(registrationId, authenticatedUser);
    }

    @GetMapping("/me")
    public List<RegistrationResponse> getMyRegistrations(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return registrationService.getMyRegistrations(authenticatedUser);
    }

    @GetMapping("/events/{eventId}")
    public List<RegistrationResponse> getEventRegistrations(@PathVariable Long eventId,
                                                            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return registrationService.getEventRegistrations(eventId, authenticatedUser);
    }

    @DeleteMapping("/{registrationId}")
    public RegistrationResponse cancelRegistration(@PathVariable Long registrationId,
                                                   @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return registrationService.cancelRegistration(registrationId, authenticatedUser);
    }

}
