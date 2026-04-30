package com.event.registrationservice.repository;

import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.entity.RegistrationStatus;
import com.event.registrationservice.dto.response.RegistrationCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByEventIdAndParticipantIdAndStatus(Long eventId, Long participantId, RegistrationStatus status);

    Optional<Registration> findByIdAndParticipantId(Long id, Long participantId);

    List<Registration> findAllByParticipantIdOrderByRegisteredAtDesc(Long participantId);

    List<Registration> findAllByEventIdOrderByRegisteredAtAsc(Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    @Query("""
            select r.eventId as eventId, count(r) as registeredCount
            from Registration r
            where r.eventId in :eventIds and r.status = :status
            group by r.eventId
            """)
    List<RegistrationCountProjection> findRegisteredCountsByEventIdsAndStatus(List<Long> eventIds,
                                                                              RegistrationStatus status);
}
