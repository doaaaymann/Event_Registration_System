package com.event.registrationservice.controller;

import com.event.registrationservice.dto.response.RegistrationCountResponse;
import com.event.registrationservice.service.RegistrationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationQueryControllerTest {

    @Mock
    private RegistrationQueryService registrationQueryService;

    @InjectMocks
    private RegistrationQueryController registrationQueryController;

    @Test
    void getRegistrationCountReturnsOkResponse() {
        RegistrationCountResponse response = new RegistrationCountResponse(10L, 35L);
        when(registrationQueryService.getRegistrationCount(10L)).thenReturn(response);

        var result = registrationQueryController.getRegistrationCount(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(registrationQueryService).getRegistrationCount(10L);
    }
}
