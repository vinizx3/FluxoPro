CREATE TABLE notification (
    id             UUID PRIMARY KEY,
    company_id     UUID        NOT NULL REFERENCES company (id),
    user_id        UUID        NOT NULL,
    type           VARCHAR(30) NOT NULL CHECK (type IN ('STALLED_OPPORTUNITY')),
    opportunity_id UUID        NOT NULL,
    stalled_since  TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    read_at        TIMESTAMPTZ,
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id, company_id) REFERENCES users (id, company_id),
    CONSTRAINT fk_notification_opportunity
        FOREIGN KEY (opportunity_id, company_id) REFERENCES opportunity (id, company_id),
    -- One alert per stall: the database itself prevents duplicates.
    CONSTRAINT uq_notification_stall UNIQUE (opportunity_id, type, stalled_since)
);

-- Unread inbox of a user, newest first.
CREATE INDEX idx_notification_inbox
    ON notification (user_id, created_at DESC)
    WHERE read_at IS NULL;