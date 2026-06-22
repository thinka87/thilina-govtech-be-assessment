# Database Design

Database: **PostgreSQL 16**

Schema managed by **Flyway** migrations:

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__create_initial_schema.sql` | Initial schema — all tables, indexes, constraints |
| V2 | `V2__alter_citizens_nic_optional_mobile_unique.sql` | NIC made optional (nullable); mobile number uniqueness enforced |
| V3 | `V3__add_token_version_to_users.sql` | Adds `token_version` to `users` for single-session enforcement |
| V4 | `V4__tighten_column_lengths.sql` | Tightens VARCHAR lengths across `citizens`, `users`, and `supporting_documents` |

---

## Entity Relationship Overview

```
users
  │
  │ 1:1
  ▼
citizens ──────────────────────────────────────────────────┐
  │                                                         │
  │ 1:N                                                     │
  ▼                                                         │
service_requests                                            │
  │         │                                               │
  │ 1:N     │ 1:N                                           │
  ▼         ▼                                               │
supporting_ status_                                         │
documents   history                                         │
                                                            │
notifications ──────────────────────────────────────────── ┘
  (linked to citizen 1:N and service_request 1:N)
```

---

## Tables

### `users`

Stores authentication credentials for all system users (admins, agents, and citizens).

| Column                 | Type         | Constraints                         | Description                          |
|------------------------|--------------|-------------------------------------|--------------------------------------|
| `id`                   | BIGSERIAL    | PRIMARY KEY                         | Internal surrogate key               |
| `username`             | VARCHAR(150) | NOT NULL, UNIQUE                    | Email used as login identifier       |
| `password`             | VARCHAR(255) | NOT NULL                            | BCrypt-hashed password               |
| `role`                 | VARCHAR(20)  | NOT NULL                            | `ADMIN`, `SERVICE_AGENT`, `CITIZEN`  |
| `active`               | BOOLEAN      | NOT NULL, DEFAULT true              | Whether login is permitted           |
| `must_change_password` | BOOLEAN      | NOT NULL, DEFAULT false             | Forces password change on next login |
| `token_version`        | INTEGER      | NOT NULL, DEFAULT 0                 | Incremented on each login; embedded as `ver` claim in JWT to enforce single-session |
| `created_at`           | TIMESTAMP    | NOT NULL                            | Account creation timestamp           |
| `updated_at`           | TIMESTAMP    |                                     | Last update timestamp                |

**Indexes:** `uq_users_username` (unique)

---

### `citizens`

Extended profile for users with the CITIZEN role. Every citizen has exactly one `users` record.

| Column              | Type         | Constraints                         | Description                              |
|---------------------|--------------|-------------------------------------|------------------------------------------|
| `id`                | BIGSERIAL    | PRIMARY KEY                         | Internal surrogate key                   |
| `citizen_reference` | VARCHAR(100) | NOT NULL, UNIQUE                    | Business key (e.g., `CIT-ABCD1234`)     |
| `user_id`           | BIGINT       | NOT NULL, UNIQUE, FK → users(id)    | Linked user account (1:1)                |
| `name`              | VARCHAR(200) | NOT NULL                            | Full name                                |
| `nic`               | VARCHAR(12)  | UNIQUE, NULLABLE                    | National Identity Card number (optional) |
| `email`             | VARCHAR(150) | NOT NULL                            | Contact email (same as username)         |
| `mobile_number`     | VARCHAR(10)  | NOT NULL, UNIQUE                    | Mobile phone number (exactly 10 digits)  |
| `address`           | TEXT         | NOT NULL                            | Residential address                      |
| `status`            | VARCHAR(20)  | NOT NULL, DEFAULT 'ACTIVE'          | `ACTIVE` or `INACTIVE`                  |
| `created_at`        | TIMESTAMP    | NOT NULL                            |                                          |
| `updated_at`        | TIMESTAMP    |                                     |                                          |

**Indexes:** `idx_citizens_citizen_reference`, `idx_citizens_nic`, `idx_citizens_user_id`

**Constraints:** `uq_citizens_citizen_reference`, `uq_citizens_user_id`, `uq_citizens_nic`, `uq_citizens_mobile_number`

**Notes:**
- Citizen deactivation is a soft delete — sets `status = 'INACTIVE'` and `users.active = false`. Records are retained.
- `nic` is **optional** (nullable). When provided, it must be unique across all citizens. PostgreSQL treats each `NULL` as distinct, so multiple citizens may have no NIC without violating the unique constraint.
- Accepted NIC formats (validated at the application layer):
  - **Old format:** 9 digits followed by `V` or `X` (case-insensitive), e.g. `871840504V` (10 chars)
  - **New format:** exactly 12 digits, e.g. `200012345678`
- `mobile_number` must be **exactly 10 digits** and **unique** across all citizens. Enforced at both the database level (`uq_citizens_mobile_number`) and the application layer.
- The `user_id` UNIQUE constraint enforces the 1:1 relationship with `users`.

---

### `service_requests`

A service request submitted by a citizen.

| Column              | Type         | Constraints                         | Description                          |
|---------------------|--------------|-------------------------------------|--------------------------------------|
| `id`                | BIGSERIAL    | PRIMARY KEY                         | Internal surrogate key               |
| `request_reference` | VARCHAR(100) | NOT NULL, UNIQUE                    | Business key (e.g., `REQ-ABCD1234`) |
| `citizen_id`        | BIGINT       | NOT NULL, FK → citizens(id)         | Submitting citizen                   |
| `service_type`      | VARCHAR(100) | NOT NULL                            | Type of government service requested |
| `description`       | TEXT         | NOT NULL                            | Details of the request               |
| `status`            | VARCHAR(50)  | NOT NULL, DEFAULT 'SUBMITTED'       | Current lifecycle status             |
| `created_at`        | TIMESTAMP    | NOT NULL                            |                                      |
| `updated_at`        | TIMESTAMP    |                                     |                                      |

**Indexes:** `idx_service_requests_request_reference`, `idx_service_requests_citizen_id`, `idx_service_requests_status`

**Status values:** `SUBMITTED`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `CANCELLED`

**Allowed transitions:**

| From      | To        | Actor         |
|-----------|-----------|---------------|
| SUBMITTED | IN_REVIEW | SERVICE_AGENT |
| IN_REVIEW | APPROVED  | SERVICE_AGENT |
| IN_REVIEW | REJECTED  | SERVICE_AGENT |
| SUBMITTED | CANCELLED | ADMIN         |
| IN_REVIEW | CANCELLED | ADMIN         |

`APPROVED`, `REJECTED`, and `CANCELLED` are **terminal** — no further transitions are permitted.

---

### `supporting_documents`

Metadata for documents attached to a service request. Actual binary file storage is out of scope for this assessment; only document metadata is persisted.

| Column               | Type         | Constraints                         | Description                          |
|----------------------|--------------|-------------------------------------|--------------------------------------|
| `id`                 | BIGSERIAL    | PRIMARY KEY                         | Internal surrogate key               |
| `document_reference` | VARCHAR(100) | NOT NULL, UNIQUE                    | Business key (e.g., `DOC-ABCD1234`) |
| `service_request_id` | BIGINT       | NOT NULL, FK → service_requests(id) | Owning service request               |
| `document_type`      | VARCHAR(100) | NOT NULL                            | Category (e.g., `NIC_COPY`, `BIRTH_CERTIFICATE`) |
| `document_name`      | VARCHAR(200) | NOT NULL                            | Descriptive label provided by the citizen |
| `verification_status`| VARCHAR(20)  | NOT NULL, DEFAULT 'PENDING'         | `PENDING`, `VERIFIED`, `REJECTED`    |
| `created_at`         | TIMESTAMP    | NOT NULL                            |                                      |
| `updated_at`         | TIMESTAMP    |                                     |                                      |

**Indexes:** `idx_supporting_docs_document_reference`, `idx_supporting_docs_service_request_id`

**Notes:**
- Document records can be hard-deleted by an admin (unlike citizens and service requests which are soft-deleted).
- Adding documents or updating verification status is blocked (`409 Conflict`) when the parent service request has status `CANCELLED`.
- Binary file storage (e.g., S3, local FS) is not implemented. A `storage_reference` or `file_path` column would be added when file upload is introduced.

---

### `notifications`

One notification is created for every status change on a service request.

| Column              | Type         | Constraints                         | Description                          |
|---------------------|--------------|-------------------------------------|--------------------------------------|
| `id`                | BIGSERIAL    | PRIMARY KEY                         | Internal surrogate key (used as notification ID) |
| `citizen_id`        | BIGINT       | NOT NULL, FK → citizens(id)         | Recipient citizen                    |
| `service_request_id`| BIGINT       | NOT NULL, FK → service_requests(id) | Associated service request           |
| `message`           | TEXT         | NOT NULL                            | Notification text                    |
| `status`            | VARCHAR(50)  | NOT NULL, DEFAULT 'UNREAD'          | `UNREAD` or `READ`                  |
| `created_at`        | TIMESTAMP    | NOT NULL                            |                                      |

**Indexes:** `idx_notifications_citizen_id`, `idx_notifications_service_request_id`

**Notes:**
- Notifications are never deleted, only marked as `READ`.
- One notification is created each time a service request transitions status (SUBMITTED → IN_REVIEW, IN_REVIEW → APPROVED/REJECTED, etc.).

---

### `status_history`

Immutable audit trail. One record per status transition.

| Column               | Type         | Constraints                         | Description                          |
|----------------------|--------------|-------------------------------------|--------------------------------------|
| `id`                 | BIGSERIAL    | PRIMARY KEY                         |                                      |
| `service_request_id` | BIGINT       | NOT NULL, FK → service_requests(id) | The service request                  |
| `old_status`         | VARCHAR(50)  | NULLABLE                            | Previous status (`NULL` for initial SUBMITTED) |
| `new_status`         | VARCHAR(50)  | NOT NULL                            | Status after transition              |
| `changed_by`         | VARCHAR(150) | NULLABLE                            | Username of user who made the change |
| `remarks`            | TEXT         |                                     | Optional notes from the agent        |
| `changed_at`         | TIMESTAMP    | NOT NULL                            | When the transition occurred         |

**Indexes:** `idx_status_history_service_request_id`

**Notes:**
- `old_status` is `NULL` for the very first entry when a request is created (SUBMITTED state has no prior status).
- Records are never updated or deleted — this table is append-only.

---

## Design Decisions

### Surrogate + Business Keys

Every table uses an auto-incremented `BIGSERIAL` as the primary key for joins, plus a separately generated business key (`citizen_reference`, `request_reference`, etc.) exposed in the API. This decouples internal IDs from API surface area and allows the reference format to change without affecting database integrity.

### Soft Delete for Citizens and Service Requests

Citizens are deactivated (not deleted) to preserve data integrity across related tables (`service_requests`, `notifications`, `status_history`). Service requests are cancelled (not deleted) to maintain the full audit trail.

### Separation of `users` and `citizens`

The `users` table is the authentication identity. The `citizens` table is the business profile. This separation allows the system to onboard admin and agent accounts without creating citizen profiles, and leaves room for future user types without altering the citizen schema.

### Append-Only `status_history`

Status history records are never modified. This provides a tamper-evident audit trail that fully reconstructs the lifecycle of any service request.

### Optional NIC with Sparse Uniqueness

NIC was made nullable (V2 migration) to support citizens who may not yet have a NIC or whose NIC is unknown at registration time. The `UNIQUE` constraint is retained — PostgreSQL's behaviour of treating each `NULL` as distinct means the constraint only fires when two non-null NIC values collide. Sri Lankan NIC format validation (old: 9 digits + V/X; new: 12 digits) is enforced in the application layer to allow format rules to evolve without a schema migration.

### Mobile Number Uniqueness

Mobile number uniqueness is enforced at both the database level (`uq_citizens_mobile_number` constraint, added in V2) and the application layer (`CitizenService` pre-save check). The application-layer check produces a user-friendly `409 Conflict` response; the database constraint is a safety net against race conditions.

### Single-Session Enforcement via `token_version`

`users.token_version` (added in V3) is incremented on every successful login. The current value is embedded as a `ver` claim in the issued JWT. The JWT filter rejects any request whose token `ver` claim does not match the current `token_version` in the database, instantly invalidating all previously issued tokens for that user.

### Document Metadata Only

`supporting_documents` stores only metadata (type, name, verification status). Actual binary file storage is out of scope for this assessment. To add file upload, a `storage_reference` column pointing to an external store (e.g., Amazon S3) and a `mime_type` column would be added in a future migration.
