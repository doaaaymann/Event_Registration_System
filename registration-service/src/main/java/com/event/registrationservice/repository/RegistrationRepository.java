package com.event.registrationservice.repository;

import com.event.registrationservice.entity.Registration;
import com.event.registrationservice.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

<<<<<<< HEAD
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

=======
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<Registration> findByIdAndParticipantId(Long id, Long participantId);

    List<Registration> findAllByParticipantIdOrderByRegisteredAtDesc(Long participantId);

    List<Registration> findAllByEventIdOrderByRegisteredAtAsc(Long eventId);

>>>>>>> origin/Registration
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
