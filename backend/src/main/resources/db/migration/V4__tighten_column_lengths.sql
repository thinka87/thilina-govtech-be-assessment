-- =============================================================
-- V4__tighten_column_lengths.sql
-- Tightens column lengths across citizens, users, and
-- supporting_documents tables to match actual data constraints
-- enforced at the application layer.
--
-- citizens:
--   mobile_number        VARCHAR(10)  — DTO enforces exactly 10 digits
--   nic                  VARCHAR(12)  — Sri Lankan NIC max: 12 chars (new format);
--                                       old format: 9 digits + V/X = 10 chars
--   status               VARCHAR(20)  — current values: ACTIVE (6), INACTIVE (8);
--                                       headroom for future enum additions
-- users:
--   role                 VARCHAR(20)  — longest value: SERVICE_AGENT (13 chars);
--                                       headroom for future role additions
-- supporting_documents:
--   verification_status  VARCHAR(20)  — values: PENDING (7), VERIFIED/REJECTED (8);
--                                       headroom for future enum additions
--
-- All changes are safe: application-layer validation already enforces
-- these bounds, so no existing data can exceed the new column sizes.
-- =============================================================

ALTER TABLE citizens             ALTER COLUMN mobile_number       TYPE VARCHAR(10);
ALTER TABLE citizens             ALTER COLUMN nic                 TYPE VARCHAR(12);
ALTER TABLE citizens             ALTER COLUMN status              TYPE VARCHAR(20);
ALTER TABLE users                ALTER COLUMN role                TYPE VARCHAR(20);
ALTER TABLE supporting_documents ALTER COLUMN verification_status TYPE VARCHAR(20);
