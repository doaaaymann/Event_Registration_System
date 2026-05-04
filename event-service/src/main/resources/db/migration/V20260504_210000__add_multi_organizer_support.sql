ALTER TABLE events
    ADD COLUMN IF NOT EXISTS organizer_ids TEXT;

UPDATE events
SET organizer_ids = organizer_id::text
WHERE organizer_ids IS NULL OR organizer_ids = '';

ALTER TABLE events
    ALTER COLUMN organizer_ids SET NOT NULL;
