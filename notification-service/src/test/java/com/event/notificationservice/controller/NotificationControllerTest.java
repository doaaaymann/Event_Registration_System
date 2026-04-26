package com.event.notificationservice.controller;

import com.event.notificationservice.dto.request.CreateNotificationRequest;
import com.event.notificationservice.dto.response.NotificationResponse;
import com.event.notificationservice.exception.GlobalExceptionHandler;
import com.event.notificationservice.exception.ResourceNotFoundException;
import com.event.notificationservice.security.AuthenticatedUser;
import com.event.notificationservice.security.JwtAuthenticationFilter;
import com.event.notificationservice.service.NotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createNotificationReturnsCreatedResponse() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(1L);
        request.setType("REGISTRATION_CONFIRMED");
        request.setTitle("Registration Confirmed");
        request.setMessage("You are registered for Spring Boot Workshop");

        when(notificationService.createNotification(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new NotificationResponse(
                        501L,
                        1L,
                        "REGISTRATION_CONFIRMED",
                        "Registration Confirmed",
                        "You are registered for Spring Boot Workshop",
                        false,
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/notifications")
                        .with(authentication(TestAuthenticationFactory.authentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.type").value("REGISTRATION_CONFIRMED"));
    }

    @Test
    void getNotificationsByUserIdReturnsList() throws Exception {
        when(notificationService.getNotificationsByUserId(nullable(AuthenticatedUser.class), eq(1L)))
                .thenReturn(List.of(
                        new NotificationResponse(501L, 1L, "REGISTRATION_CONFIRMED", "Registration Confirmed",
                                "You are registered for Spring Boot Workshop", false, LocalDateTime.now()),
                        new NotificationResponse(502L, 1L, "EVENT_RESCHEDULED", "Event Rescheduled",
                                "Spring Boot Workshop was rescheduled", true, LocalDateTime.now().minusHours(1))
                ));

        mockMvc.perform(get("/api/notifications/users/1")
                        .with(authentication(TestAuthenticationFactory.authentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void markAsReadReturnsNotFoundErrorBody() throws Exception {
        when(notificationService.markAsRead(nullable(AuthenticatedUser.class), eq(999L)))
                .thenThrow(new ResourceNotFoundException("Notification not found"));

        mockMvc.perform(patch("/api/notifications/999/read")
                        .with(authentication(TestAuthenticationFactory.authentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification not found"))
                .andExpect(jsonPath("$.path").value("/api/notifications/999/read"));
    }
}
