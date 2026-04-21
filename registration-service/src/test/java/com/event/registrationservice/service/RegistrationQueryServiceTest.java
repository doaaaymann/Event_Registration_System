package com.event.registrationservice.service;

import com.event.registrationservice.dto.response.RegistrationCountResponse;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationQueryServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private RegistrationQueryService registrationQueryService;

    @Test
    void getRegistrationCountReturnsRegisteredCountOnly() {
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED)).thenReturn(35L);

        RegistrationCountResponse response = registrationQueryService.getRegistrationCount(10L);

        assertThat(response.getEventId()).isEqualTo(10L);
        assertThat(response.getRegisteredCount()).isEqualTo(35L);
        verify(registrationRepository).countByEventIdAndStatus(10L, RegistrationStatus.REGISTERED);
    }
}
