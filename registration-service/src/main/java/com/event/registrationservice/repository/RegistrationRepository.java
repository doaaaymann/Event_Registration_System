package com.event.registrationservice.repository;

import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
