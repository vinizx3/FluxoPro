CREATE TABLE funnel_stage_entry (
    company_id     UUID          NOT NULL REFERENCES company (id),
    stage          VARCHAR(20)   NOT NULL,
    entries        BIGINT        NOT NULL DEFAULT 0,
    entered_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, stage)
);

CREATE TABLE processed_event (
    consumer     VARCHAR(50) NOT NULL,
    event_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer, event_id)
);