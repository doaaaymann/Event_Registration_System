package com.event.registrationservice.service;

import com.event.registrationservice.dto.response.RegistrationCountResponse;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegistrationQueryService {

    private final RegistrationRepository registrationRepository;

    public RegistrationQueryService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public RegistrationCountResponse getRegistrationCount(Long eventId) {
        long registeredCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
        return new RegistrationCountResponse(eventId, registeredCount);
    }
}
