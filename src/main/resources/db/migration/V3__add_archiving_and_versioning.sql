ALTER TABLE customer
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE opportunity
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

DROP INDEX idx_customer_company;
CREATE INDEX idx_customer_company_name ON customer (company_id, name) WHERE archived_at IS NULL;

DROP INDEX idx_opportunity_board;
CREATE INDEX idx_opportunity_board ON opportunity (company_id, status, status_changed_at) WHERE archived_at IS NULL;