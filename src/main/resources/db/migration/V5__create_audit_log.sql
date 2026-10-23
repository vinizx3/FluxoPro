CREATE TABLE audit_log (
    id           UUID PRIMARY KEY,
    company_id   UUID        NOT NULL REFERENCES company (id),
    actor_id     UUID        NOT NULL,
    entity_type  VARCHAR(20) NOT NULL
        CHECK (entity_type IN ('CUSTOMER', 'OPPORTUNITY', 'PROJECT')),
    entity_id    UUID        NOT NULL,
    action       VARCHAR(20) NOT NULL
        CHECK (action IN ('CREATED', 'UPDATED', 'STATUS_CHANGED', 'ARCHIVED')),
    occurred_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_id, company_id) REFERENCES users (id, company_id)
);

CREATE INDEX idx_audit_entity
    ON audit_log (company_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_company_time
    ON audit_log (company_id, occurred_at DESC);

CREATE TABLE audit_log_change (
    audit_log_id UUID         NOT NULL REFERENCES audit_log (id),
    field        VARCHAR(100) NOT NULL,
    old_value    TEXT,
    new_value    TEXT
);

CREATE INDEX idx_audit_change_log ON audit_log_change (audit_log_id);

-- Audit records are append-only: the database refuses to rewrite or delete history.
CREATE FUNCTION forbid_audit_changes() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit records are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION forbid_audit_changes();

CREATE TRIGGER trg_audit_change_append_only
    BEFORE UPDATE OR DELETE ON audit_log_change
    FOR EACH ROW EXECUTE FUNCTION forbid_audit_changes();