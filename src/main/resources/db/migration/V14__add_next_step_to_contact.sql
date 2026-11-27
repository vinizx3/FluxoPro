ALTER TABLE opportunity_contact
    ADD COLUMN next_step_at   DATE,
    ADD COLUMN next_step_note VARCHAR(255);