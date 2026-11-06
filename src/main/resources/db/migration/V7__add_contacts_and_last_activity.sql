ALTER TABLE opportunity
    ADD COLUMN last_activity_at TIMESTAMPTZ,
    ADD COLUMN next_step_at     DATE,
    ADD COLUMN next_step_note   VARCHAR(255);

UPDATE opportunity SET last_activity_at = status_changed_at;

ALTER TABLE opportunity ALTER COLUMN last_activity_at SET NOT NULL;

CREATE INDEX idx_opportunity_last_activity
    ON opportunity (company_id, last_activity_at)
    WHERE archived_at IS NULL;

CREATE TABLE opportunity_contact (
    id             UUID PRIMARY KEY,
    company_id     UUID          NOT NULL REFERENCES company (id),
    opportunity_id UUID          NOT NULL,
    author_id      UUID          NOT NULL,
    kind           VARCHAR(20)   NOT NULL CHECK (kind IN ('CALL', 'WHATSAPP', 'EMAIL', 'MEETING', 'OTHER')),
    note           VARCHAR(1000) NOT NULL,
    contacted_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_contact_opportunity
        FOREIGN KEY (opportunity_id, company_id) REFERENCES opportunity (id, company_id),
    CONSTRAINT fk_contact_author
        FOREIGN KEY (author_id, company_id) REFERENCES users (id, company_id)
);

CREATE INDEX idx_contact_opportunity
    ON opportunity_contact (opportunity_id, contacted_at DESC);