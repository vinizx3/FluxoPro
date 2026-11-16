-- Changes to company settings (for example the stalled threshold) are recorded in the audit log,
-- so the entity_type check must accept COMPANY. The V5 check was created inline, so PostgreSQL
-- named it audit_log_entity_type_check.
ALTER TABLE audit_log DROP CONSTRAINT audit_log_entity_type_check;

ALTER TABLE audit_log
    ADD CONSTRAINT audit_log_entity_type_check
        CHECK (entity_type IN ('CUSTOMER', 'OPPORTUNITY', 'PROJECT', 'COMPANY'));
