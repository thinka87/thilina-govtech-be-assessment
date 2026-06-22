-- =============================================================
-- V1__create_initial_schema.sql
-- Digital Government Service Request Platform — Initial Schema
-- Database: PostgreSQL 16
-- =============================================================

-- =============================================
-- TABLE: users
-- Stores authentication and authorisation data
-- for all roles: CITIZEN, SERVICE_AGENT, ADMIN.
-- =============================================
CREATE TABLE users (
    id                   BIGSERIAL       PRIMARY KEY,
    username             VARCHAR(150)    NOT NULL,
    password             VARCHAR(255)    NOT NULL,
    role                 VARCHAR(50)     NOT NULL,
    active               BOOLEAN         NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP       NOT NULL,
    updated_at           TIMESTAMP,

    CONSTRAINT uq_users_username UNIQUE (username)
);

COMMENT ON TABLE  users                      IS 'Authentication and authorisation accounts for all platform users.';
COMMENT ON COLUMN users.username             IS 'Unique login identifier. For citizens this is the citizen email.';
COMMENT ON COLUMN users.password             IS 'BCrypt-hashed password. Never stored in plaintext.';
COMMENT ON COLUMN users.role                 IS 'CITIZEN | SERVICE_AGENT | ADMIN';
COMMENT ON COLUMN users.must_change_password IS 'TRUE for newly created citizen accounts using a temporary password.';


-- =============================================
-- TABLE: citizens
-- Citizen profile and business data.
-- Each citizen has exactly one linked user account (1-to-1).
-- =============================================
CREATE TABLE citizens (
    id                 BIGSERIAL       PRIMARY KEY,
    citizen_reference  VARCHAR(100)    NOT NULL,
    user_id            BIGINT          NOT NULL,
    name               VARCHAR(200)    NOT NULL,
    nic                VARCHAR(50)     NOT NULL,
    email              VARCHAR(150)    NOT NULL,
    mobile_number      VARCHAR(30)     NOT NULL,
    address            TEXT            NOT NULL,
    status             VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMP       NOT NULL,
    updated_at         TIMESTAMP,

    CONSTRAINT uq_citizens_citizen_reference UNIQUE (citizen_reference),
    CONSTRAINT uq_citizens_user_id           UNIQUE (user_id),
    CONSTRAINT uq_citizens_nic               UNIQUE (nic),
    CONSTRAINT fk_citizens_user              FOREIGN KEY (user_id) REFERENCES users (id)
);

COMMENT ON TABLE  citizens                    IS 'Citizen profile records linked 1-to-1 to a user account.';
COMMENT ON COLUMN citizens.citizen_reference  IS 'Human-readable system reference, e.g. CIT-20240001.';
COMMENT ON COLUMN citizens.user_id            IS 'FK to users.id (1-to-1 enforced by unique constraint).';
COMMENT ON COLUMN citizens.nic                IS 'National Identity Card number — globally unique.';
COMMENT ON COLUMN citizens.email              IS 'Also used as users.username for the linked account.';
COMMENT ON COLUMN citizens.status             IS 'ACTIVE | INACTIVE';


-- =============================================
-- TABLE: service_requests
-- A citizen's formal request for a government service.
-- =============================================
CREATE TABLE service_requests (
    id                BIGSERIAL       PRIMARY KEY,
    request_reference VARCHAR(100)    NOT NULL,
    citizen_id        BIGINT          NOT NULL,
    service_type      VARCHAR(100)    NOT NULL,
    description       TEXT            NOT NULL,
    status            VARCHAR(50)     NOT NULL DEFAULT 'SUBMITTED',
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,

    CONSTRAINT uq_service_requests_request_reference UNIQUE (request_reference),
    CONSTRAINT fk_service_requests_citizen           FOREIGN KEY (citizen_id) REFERENCES citizens (id)
);

COMMENT ON TABLE  service_requests                   IS 'Citizen service requests and their current lifecycle status.';
COMMENT ON COLUMN service_requests.request_reference IS 'Human-readable system reference, e.g. SR-20240001.';
COMMENT ON COLUMN service_requests.service_type      IS 'Category of service, e.g. PASSPORT, BIRTH_CERTIFICATE.';
COMMENT ON COLUMN service_requests.status            IS 'SUBMITTED | IN_REVIEW | APPROVED | REJECTED | CANCELLED';


-- =============================================
-- TABLE: supporting_documents
-- Metadata for documents attached to a service request.
-- Binary storage is out-of-scope; add a storage_path column
-- when integrating with S3 or local file storage.
-- =============================================
CREATE TABLE supporting_documents (
    id                   BIGSERIAL       PRIMARY KEY,
    document_reference   VARCHAR(100)    NOT NULL,
    service_request_id   BIGINT          NOT NULL,
    document_type        VARCHAR(100)    NOT NULL,
    document_name        VARCHAR(200)    NOT NULL,
    verification_status  VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP       NOT NULL,
    updated_at           TIMESTAMP,

    CONSTRAINT uq_supporting_documents_document_reference UNIQUE (document_reference),
    CONSTRAINT fk_supporting_documents_service_request    FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
);

COMMENT ON TABLE  supporting_documents                     IS 'Metadata for documents submitted with a service request.';
COMMENT ON COLUMN supporting_documents.document_reference  IS 'Human-readable system reference, e.g. DOC-20240001.';
COMMENT ON COLUMN supporting_documents.document_type       IS 'e.g. NATIONAL_ID, PROOF_OF_ADDRESS, BIRTH_CERTIFICATE.';
COMMENT ON COLUMN supporting_documents.verification_status IS 'PENDING | VERIFIED | REJECTED';


-- =============================================
-- TABLE: notifications
-- In-app notifications sent to citizens on status changes.
-- Append-only; only the status column is ever updated.
-- =============================================
CREATE TABLE notifications (
    id                  BIGSERIAL       PRIMARY KEY,
    citizen_id          BIGINT          NOT NULL,
    service_request_id  BIGINT          NOT NULL,
    message             TEXT            NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'UNREAD',
    created_at          TIMESTAMP       NOT NULL,

    CONSTRAINT fk_notifications_citizen         FOREIGN KEY (citizen_id)         REFERENCES citizens (id),
    CONSTRAINT fk_notifications_service_request FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
);

COMMENT ON TABLE  notifications        IS 'In-app notifications delivered to citizens. Append-only except for status.';
COMMENT ON COLUMN notifications.status IS 'UNREAD | READ';


-- =============================================
-- TABLE: status_history
-- Immutable audit trail of service request status transitions.
-- Never updated or deleted after insert.
-- =============================================
CREATE TABLE status_history (
    id                  BIGSERIAL       PRIMARY KEY,
    service_request_id  BIGINT          NOT NULL,
    old_status          VARCHAR(50),
    new_status          VARCHAR(50)     NOT NULL,
    changed_by          VARCHAR(150),
    changed_at          TIMESTAMP       NOT NULL,
    remarks             TEXT,

    CONSTRAINT fk_status_history_service_request FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
);

COMMENT ON TABLE  status_history            IS 'Immutable chronological audit trail of status transitions for service requests.';
COMMENT ON COLUMN status_history.old_status IS 'NULL for the initial SUBMITTED entry (no prior state).';
COMMENT ON COLUMN status_history.changed_by IS 'Username of the user who performed the transition.';


-- =============================================
-- INDEXES
-- Covering the most common query patterns
-- identified from the repository interfaces.
-- =============================================

-- citizens
CREATE INDEX idx_citizens_citizen_reference        ON citizens (citizen_reference);
CREATE INDEX idx_citizens_nic                       ON citizens (nic);
CREATE INDEX idx_citizens_user_id                   ON citizens (user_id);

-- service_requests
CREATE INDEX idx_service_requests_request_reference ON service_requests (request_reference);
CREATE INDEX idx_service_requests_citizen_id        ON service_requests (citizen_id);
CREATE INDEX idx_service_requests_status            ON service_requests (status);

-- supporting_documents
CREATE INDEX idx_supporting_docs_document_reference ON supporting_documents (document_reference);
CREATE INDEX idx_supporting_docs_service_request_id ON supporting_documents (service_request_id);

-- notifications
CREATE INDEX idx_notifications_citizen_id           ON notifications (citizen_id);
CREATE INDEX idx_notifications_service_request_id   ON notifications (service_request_id);

-- status_history
CREATE INDEX idx_status_history_service_request_id  ON status_history (service_request_id);
