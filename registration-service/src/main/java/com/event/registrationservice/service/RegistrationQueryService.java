package com.event.registrationservice.service;

import com.event.registrationservice.dto.response.RegistrationCountResponse;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<RegistrationCountResponse> getRegistrationCounts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = eventIds.stream().distinct().toList();
        Map<Long, Long> countByEventId = new LinkedHashMap<>();
        distinctIds.forEach(eventId -> countByEventId.put(eventId, 0L));

        registrationRepository.findRegisteredCountsByEventIdsAndStatus(distinctIds, RegistrationStatus.REGISTERED)
                .forEach(result -> countByEventId.put(result.getEventId(), result.getRegisteredCount()));

        return countByEventId.entrySet().stream()
                .map(entry -> new RegistrationCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
