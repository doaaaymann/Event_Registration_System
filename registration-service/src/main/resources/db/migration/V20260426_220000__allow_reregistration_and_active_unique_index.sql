ALTER TABLE registrations
    DROP CONSTRAINT IF EXISTS uk_registrations_event_participant;

CREATE UNIQUE INDEX IF NOT EXISTS uk_registrations_event_participant_active
    ON registrations (event_id, participant_id)
    WHERE status = 'REGISTERED';
