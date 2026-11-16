ALTER TABLE company
    ADD COLUMN stalled_after_days INTEGER NOT NULL DEFAULT 7
        CONSTRAINT chk_company_stalled_after_days CHECK (stalled_after_days BETWEEN 1 AND 90);