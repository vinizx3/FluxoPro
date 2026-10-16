ALTER TABLE users ADD CONSTRAINT uq_users_id_company UNIQUE (id, company_id);

CREATE TABLE customer (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL REFERENCES company (id),
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(30),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_customer_id_company UNIQUE (id, company_id)
);

CREATE INDEX idx_customer_company ON customer (company_id);

CREATE TABLE opportunity (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id         UUID         NOT NULL REFERENCES company (id),
    customer_id        UUID         NOT NULL,
    owner_id           UUID         NOT NULL,
    title              VARCHAR(200) NOT NULL,
    amount             NUMERIC(14,2),
    status             VARCHAR(20)  NOT NULL
        CHECK (status IN ('LEAD', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')),
    lost_reason        VARCHAR(255),
    status_changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_opportunity_customer
        FOREIGN KEY (customer_id, company_id) REFERENCES customer (id, company_id),
    CONSTRAINT fk_opportunity_owner
        FOREIGN KEY (owner_id, company_id) REFERENCES users (id, company_id)
);

CREATE INDEX idx_opportunity_board ON opportunity (company_id, status, status_changed_at);
CREATE INDEX idx_opportunity_customer ON opportunity (customer_id);