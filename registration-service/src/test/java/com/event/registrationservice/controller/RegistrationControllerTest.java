package com.event.registrationservice.controller;

import com.event.registrationservice.dto.request.CreateRegistrationRequest;
import com.event.registrationservice.dto.response.RegistrationResponse;
import com.event.registrationservice.exception.GlobalExceptionHandler;
import com.event.registrationservice.exception.ResourceNotFoundException;
import com.event.registrationservice.security.AuthenticatedUser;
import com.event.registrationservice.security.JwtAuthenticationFilter;
import com.event.registrationservice.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createRegistrationReturnsCreatedResponse() throws Exception {
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEventId(10L);

        when(registrationService.createRegistration(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new RegistrationResponse(100L, 10L, 1L, "Ali Hassan", "REGISTERED", LocalDateTime.now(), null));

        mockMvc.perform(post("/api/registrations")
                        .with(authentication(TestAuthenticationFactory.authentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.eventId").value(10L))
                .andExpect(jsonPath("$.participantId").value(1L))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    @Test
    void getRegistrationReturnsNotFoundErrorBody() throws Exception {
        when(registrationService.getRegistration(eq(999L), nullable(AuthenticatedUser.class)))
                .thenThrow(new ResourceNotFoundException("Registration not found"));

        mockMvc.perform(get("/api/registrations/999")
                        .with(authentication(TestAuthenticationFactory.authentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Registration not found"))
                .andExpect(jsonPath("$.path").value("/api/registrations/999"));
    }

    @Test
    void getMyRegistrationsReturnsList() throws Exception {
        when(registrationService.getMyRegistrations(nullable(AuthenticatedUser.class)))
                .thenReturn(List.of(
                        new RegistrationResponse(100L, 10L, 1L, "Ali Hassan", "REGISTERED", LocalDateTime.now(), null),
                        new RegistrationResponse(101L, 12L, 1L, "Ali Hassan", "CANCELLED", LocalDateTime.now().minusDays(1), LocalDateTime.now())
                ));

        mockMvc.perform(get("/api/registrations/me")
                        .with(authentication(TestAuthenticationFactory.authentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void cancelRegistrationReturnsUpdatedRegistration() throws Exception {
        when(registrationService.cancelRegistration(eq(100L), nullable(AuthenticatedUser.class)))
                .thenReturn(new RegistrationResponse(100L, 10L, 1L, "Ali Hassan", "CANCELLED", LocalDateTime.now().minusHours(1), LocalDateTime.now()));

        mockMvc.perform(delete("/api/registrations/100")
                        .with(authentication(TestAuthenticationFactory.authentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
