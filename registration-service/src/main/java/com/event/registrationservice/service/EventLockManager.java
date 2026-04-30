package com.event.registrationservice.service;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.util.function.Supplier;

@Component
public class EventLockManager {

    private final JdbcTemplate jdbcTemplate;

    public EventLockManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> T executeWithLock(Long eventId, Supplier<T> supplier) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(?)", (PreparedStatementCallback<Void>) statement -> {
            statement.setLong(1, eventId);
            statement.execute();
            return null;
        });
        try {
            return supplier.get();
        } finally {
            // Transaction-scoped advisory locks are released automatically on commit or rollback.
        }
    }
}
