CREATE TABLE sent_email (
    id       UUID PRIMARY KEY,
    event_id UUID        NOT NULL,
    user_id  UUID        NOT NULL REFERENCES users (id),
    sent_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sent_email_event_user UNIQUE (event_id, user_id)
);