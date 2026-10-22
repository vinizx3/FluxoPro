ALTER TABLE opportunity
    ADD CONSTRAINT uq_opportunity_id_company UNIQUE (id, company_id);

CREATE TABLE project (
    id                     UUID PRIMARY KEY,
    company_id             UUID         NOT NULL REFERENCES company (id),
    customer_id            UUID         NOT NULL,
    source_opportunity_id  UUID,
    owner_id               UUID         NOT NULL,
    name                   VARCHAR(200) NOT NULL,
    description            TEXT,
    status                 VARCHAR(20)  NOT NULL,
    status_changed_at      TIMESTAMPTZ  NOT NULL,
    expected_delivery_date DATE,
    created_at             TIMESTAMPTZ  NOT NULL,
    archived_at            TIMESTAMPTZ,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_status
        CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT uq_project_id_company UNIQUE (id, company_id),
    CONSTRAINT fk_project_customer
        FOREIGN KEY (customer_id, company_id) REFERENCES customer (id, company_id),
    CONSTRAINT fk_project_opportunity
        FOREIGN KEY (source_opportunity_id, company_id) REFERENCES opportunity (id, company_id),
    CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id, company_id) REFERENCES users (id, company_id)
);

-- One project per won opportunity, enforced by the database (idempotency).
CREATE UNIQUE INDEX uq_project_source_opportunity
    ON project (source_opportunity_id)
    WHERE source_opportunity_id IS NOT NULL;

CREATE INDEX ix_project_board
    ON project (company_id, status)
    WHERE archived_at IS NULL;

CREATE INDEX ix_project_customer
    ON project (customer_id, company_id)
    WHERE archived_at IS NULL;