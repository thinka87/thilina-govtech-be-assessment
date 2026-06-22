-- =============================================================
-- V3__add_token_version_to_users.sql
-- Adds token_version to users table to support single-session
-- enforcement. Each new login increments this counter; the JWT
-- embeds the version at issue time. The JWT filter rejects any
-- token whose version doesn't match the current DB value,
-- instantly invalidating all prior sessions for that user.
-- =============================================================

ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
