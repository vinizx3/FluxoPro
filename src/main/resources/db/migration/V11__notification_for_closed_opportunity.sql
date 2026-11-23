ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN ('STALLED_OPPORTUNITY', 'OPPORTUNITY_WON', 'OPPORTUNITY_LOST'));

ALTER TABLE notification ALTER COLUMN stalled_since DROP NOT NULL;
ALTER TABLE notification ADD CONSTRAINT ck_notification_stalled_since
    CHECK (type <> 'STALLED_OPPORTUNITY' OR stalled_since IS NOT NULL);

ALTER TABLE notification ADD COLUMN source_event_id UUID;
CREATE UNIQUE INDEX uq_notification_source_event
    ON notification (source_event_id, user_id) WHERE source_event_id IS NOT NULL;