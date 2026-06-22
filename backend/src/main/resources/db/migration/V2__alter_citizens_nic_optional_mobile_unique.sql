-- =============================================================
-- V2__alter_citizens_nic_optional_mobile_unique.sql
-- • NIC is now optional (nullable) — citizens can be registered
--   without a NIC; the unique constraint is retained so that
--   no two citizens can share the same NIC when one is provided.
-- • mobile_number gains a UNIQUE constraint — enforced at both
--   the DB level (here) and the application level (CitizenService).
-- =============================================================

-- NIC: allow NULL (PostgreSQL UNIQUE constraints treat each NULL
-- as distinct, so multiple citizens can have no NIC).
ALTER TABLE citizens ALTER COLUMN nic DROP NOT NULL;

-- mobile_number: must be unique across all citizens.
ALTER TABLE citizens ADD CONSTRAINT uq_citizens_mobile_number UNIQUE (mobile_number);
