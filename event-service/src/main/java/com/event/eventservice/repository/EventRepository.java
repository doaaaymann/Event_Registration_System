package com.event.eventservice.repository;

import com.event.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrderByStartTimeAsc();

    List<Event> findByOrganizerIdOrderByStartTimeAsc(Long organizerId);
}
