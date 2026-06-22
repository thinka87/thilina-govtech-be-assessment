# API Reference

Base URL: `http://localhost:8080/api`

Interactive documentation (Swagger UI): `http://localhost:8080/api/swagger-ui/index.html`

OpenAPI JSON spec: `http://localhost:8080/api/v3/api-docs`

Timezone: All timestamps are in **Asia/Colombo (UTC+5:30)**.

---

## Authentication

### Bearer Token

All endpoints except `POST /v1/auth/login` require:

```
Authorization: Bearer <accessToken>
```

### Single Session Enforcement

Each login issues a new token and invalidates all previously issued tokens for that user. If a user logs in from a second device, the first session's token becomes invalid immediately. Requests made with an old token receive `401 Unauthorized`.

### Login

```http
POST /v1/auth/login
Content-Type: application/json
```

Request body:
```json
{
  "username": "admin@gov.lk",
  "password": "Admin@123"
}
```

Response `200 OK`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin@gov.lk",
  "role": "ADMIN",
  "mustChangePassword": false,
  "citizenReference": null
}
```

**Deactivated account** — `401 Unauthorized`:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Your account has been deactivated. Please contact the administrator."
}
```

**Invalid credentials** — `401 Unauthorized`:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password."
}
```

### Change Password

```http
PATCH /v1/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json
```

Request body:
```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@456"
}
```

**Password complexity rules** (applies to `newPassword` and citizen `temporaryPassword`):
- Minimum 8 characters
- At least one uppercase letter (A–Z)
- At least one digit (0–9)
- At least one special character (e.g. `@`, `!`, `#`, `$`)

Response `200 OK` — ApiResponse wrapper with success message.

> Citizens created via the admin API have `mustChangePassword = true`. They must call this endpoint on first login. Changing password also invalidates all existing sessions (single session enforcement).

---

## Response Wrappers

### Write operations (POST, PUT, PATCH, DELETE with body)

Successful write operations return an `ApiResponse` wrapper:

```json
{
  "success": true,
  "message": "Human-readable success message",
  "data": { ... }
}
```

### Paginated list responses

```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Direct responses

Some GET endpoints return a DTO or list directly without a wrapper (e.g., document list by service request, status history list, service types list).

---

## Endpoint Reference

### 1. Authentication — `/v1/auth`

| Method | Path                       | Auth Required | Description             |
|--------|----------------------------|---------------|-------------------------|
| POST   | `/v1/auth/login`           | No            | Obtain JWT access token |
| PATCH  | `/v1/auth/change-password` | Yes (any)     | Change own password     |

---

### 2. Citizen Management — `/v1/citizens`

| Method | Path                                       | Role                   | Description                        |
|--------|--------------------------------------------|------------------------|------------------------------------|
| POST   | `/v1/citizens`                             | ADMIN                  | Create a new citizen account       |
| GET    | `/v1/citizens`                             | ADMIN                  | List / search citizens (paginated) |
| GET    | `/v1/citizens/{ref}`                       | ADMIN, SERVICE_AGENT   | Get citizen by reference           |
| PUT    | `/v1/citizens/{ref}`                       | ADMIN                  | Update citizen profile             |
| DELETE | `/v1/citizens/{ref}`                       | ADMIN                  | Deactivate a citizen               |

**Create Citizen — POST `/v1/citizens`**

Request body:
```json
{
  "name": "Kamal Perera",
  "nic": "199012345678",
  "email": "kamal.perera@email.com",
  "mobileNumber": "0771234567",
  "address": "123 Main Street, Colombo 03",
  "temporaryPassword": "TempPass@123"
}
```

> `nic` is optional but unique when provided. `mobileNumber` is required and unique. `temporaryPassword` must meet password complexity rules.

Response `201 Created` (ApiResponse):
```json
{
  "success": true,
  "message": "Citizen created successfully",
  "data": {
    "citizenReference": "CIT-ABCD1234",
    "name": "Kamal Perera",
    "nic": "199012345678",
    "email": "kamal.perera@email.com",
    "mobileNumber": "0771234567",
    "address": "123 Main Street, Colombo 03",
    "status": "ACTIVE",
    "username": "kamal.perera@email.com",
    "mustChangePassword": true,
    "createdAt": "2026-06-22T09:00:00",
    "updatedAt": null
  }
}
```

**List Citizens — GET `/v1/citizens`**

Query parameters:
- `search` (optional) — keyword search across name, NIC, email, mobile
- `status` (optional) — `ACTIVE` | `INACTIVE`
- `page` (default 0)
- `size` (default 10)

---

### 3. Service Request Management — `/v1/service-requests`

| Method | Path                                              | Role                        | Description                              |
|--------|---------------------------------------------------|-----------------------------|------------------------------------------|
| GET    | `/v1/service-requests/types`                      | Any authenticated           | List available service types             |
| POST   | `/v1/service-requests`                            | CITIZEN, ADMIN              | Create service request                   |
| GET    | `/v1/service-requests`                            | ADMIN, SERVICE_AGENT        | Search all requests (paginated)          |
| GET    | `/v1/service-requests/{requestRef}`               | ADMIN, SERVICE_AGENT        | Get single request (full detail)         |
| PUT    | `/v1/service-requests/{requestRef}`               | SERVICE_AGENT               | Update request details (SUBMITTED only)  |
| PATCH  | `/v1/service-requests/{requestRef}/status`        | SERVICE_AGENT               | Update status                            |
| DELETE | `/v1/service-requests/{requestRef}`               | ADMIN                       | Cancel a request                         |
| GET    | `/v1/service-requests/{requestRef}/status-history`| ADMIN, SERVICE_AGENT        | Get status audit trail                   |

**Get Service Types — GET `/v1/service-requests/types`**

Response `200 OK` (plain array):
```json
[
  "PASSPORT_RENEWAL",
  "BIRTH_CERTIFICATE",
  "DRIVING_LICENSE",
  "BUSINESS_REGISTRATION",
  "OTHER"
]
```

**Create Service Request — POST `/v1/service-requests`**

CITIZEN caller (citizen derived from JWT — omit `citizenReference`):
```json
{
  "serviceType": "PASSPORT_RENEWAL",
  "description": "Renewing expired passport."
}
```

ADMIN caller (must specify `citizenReference`):
```json
{
  "serviceType": "BIRTH_CERTIFICATE",
  "description": "Requesting birth certificate copy.",
  "citizenReference": "CIT-ABCD1234"
}
```

Response `201 Created` (ApiResponse):
```json
{
  "success": true,
  "message": "Service request created successfully",
  "data": {
    "requestReference": "REQ-ABCD1234",
    "citizenReference": "CIT-ABCD1234",
    "citizenName": "Kamal Perera",
    "serviceType": "PASSPORT_RENEWAL",
    "description": "Renewing expired passport.",
    "status": "SUBMITTED",
    "createdAt": "2026-06-22T09:00:00"
  }
}
```

**Search Service Requests — GET `/v1/service-requests`**

Query parameters:
- `status` (optional) — `SUBMITTED` | `IN_REVIEW` | `APPROVED` | `REJECTED` | `CANCELLED`
- `citizenReference` (optional) — exact match
- `serviceType` (optional) — partial, case-insensitive match
- `page` (default 0), `size` (default 10)

**View Citizen's Own Requests — GET `/v1/citizens/{citizenRef}/service-requests`**

Role: CITIZEN (own requests only).

Response is `PageResponse<ServiceRequestSummaryResponse>`. Each entry includes a `documents` array with the supporting documents attached to that request:

```json
{
  "content": [
    {
      "requestReference": "REQ-ABCD1234",
      "serviceType": "PASSPORT_RENEWAL",
      "status": "SUBMITTED",
      "citizenReference": "CIT-ABCD1234",
      "createdAt": "2026-06-22T09:00:00",
      "documents": [
        {
          "documentReference": "DOC-ABCD1234",
          "documentType": "NIC_COPY",
          "documentName": "National Identity Card Copy",
          "verificationStatus": "PENDING",
          "createdAt": "2026-06-22T09:05:00"
        }
      ]
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

> `documents` is omitted (not null) when empty — only populated when documents exist.

**Status Transition Rules:**

```
SUBMITTED  →  IN_REVIEW   (SERVICE_AGENT)
IN_REVIEW  →  APPROVED    (SERVICE_AGENT)
IN_REVIEW  →  REJECTED    (SERVICE_AGENT)
SUBMITTED  →  CANCELLED   (ADMIN)
IN_REVIEW  →  CANCELLED   (ADMIN)
```

Terminal statuses (`APPROVED`, `REJECTED`, `CANCELLED`) cannot be transitioned further.

**Update Status — PATCH `/v1/service-requests/{requestRef}/status`**

Request body:
```json
{
  "status": "IN_REVIEW",
  "remarks": "Starting document verification."
}
```

Response `200 OK` (ApiResponse wrapping `StatusUpdateResponse`):
```json
{
  "success": true,
  "message": "Service request status updated successfully.",
  "data": {
    "requestReference": "REQ-ABCD1234",
    "previousStatus": "SUBMITTED",
    "newStatus": "IN_REVIEW",
    "remarks": "Starting document verification.",
    "updatedAt": "2026-06-22T09:30:00"
  }
}
```

> Attempting to transition from a terminal state (`APPROVED`, `REJECTED`, `CANCELLED`) returns `400 Bad Request`.

---

### 4. Supporting Documents — `/v1/service-requests/{requestRef}/documents` and `/v1/documents`

| Method | Path                                              | Role                        | Description                              |
|--------|---------------------------------------------------|-----------------------------|------------------------------------------|
| POST   | `/v1/service-requests/{requestRef}/documents`     | CITIZEN                     | Add document metadata to a service request |
| GET    | `/v1/service-requests/{requestRef}/documents`     | ADMIN, SERVICE_AGENT        | List documents for a request             |
| GET    | `/v1/documents/{documentRef}`                     | ADMIN, SERVICE_AGENT        | Get document by reference                |
| PUT    | `/v1/documents/{documentRef}`                     | SERVICE_AGENT               | Update document metadata                 |
| PATCH  | `/v1/documents/{documentRef}/verification-status` | SERVICE_AGENT               | Update document verification status      |
| DELETE | `/v1/documents/{documentRef}`                     | ADMIN                       | Delete document                          |

> **CANCELLED restriction**: Adding documents (`POST`) and updating verification status (`PATCH`) both return `400 Bad Request` if the parent service request has status `CANCELLED`.

**Add Document — POST `/v1/service-requests/{requestRef}/documents`**

```json
{
  "documentType": "NIC_COPY",
  "documentName": "National Identity Card Copy"
}
```

Response `201 Created` (ApiResponse):
```json
{
  "success": true,
  "message": "Document added successfully",
  "data": {
    "documentReference": "DOC-ABCD1234",
    "requestReference": "REQ-ABCD1234",
    "documentType": "NIC_COPY",
    "documentName": "National Identity Card Copy",
    "verificationStatus": "PENDING",
    "createdAt": "2026-06-22T09:05:00"
  }
}
```

**Update Verification Status — PATCH `/v1/documents/{documentRef}/verification-status`**

```json
{
  "verificationStatus": "VERIFIED",
  "remarks": "Document verified. NIC copy is clear and valid."
}
```

Allowed values: `PENDING`, `VERIFIED`, `REJECTED`.

---

### 5. Notifications — `/v1/citizens/{citizenRef}/notifications`

Role: CITIZEN (own notifications only).

| Method | Path                                        | Description                         |
|--------|---------------------------------------------|-------------------------------------|
| GET    | `/v1/citizens/{citizenRef}/notifications`   | Get notifications (paginated)       |
| PATCH  | `/v1/notifications/{id}/read`               | Mark a notification as read         |

Notifications are automatically created when a service request status changes.

**Get Notifications** response (PageResponse):
```json
{
  "content": [
    {
      "id": 1,
      "message": "Your service request REQ-ABCD1234 is now under review.",
      "status": "UNREAD",
      "createdAt": "2026-06-22T09:30:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 6. Status History — `/v1/service-requests/{requestRef}/status-history`

Role: ADMIN, SERVICE_AGENT.

Response (plain array):
```json
[
  {
    "oldStatus": null,
    "newStatus": "SUBMITTED",
    "changedBy": "kamal@email.com",
    "remarks": "Service request submitted.",
    "changedAt": "2026-06-22T09:00:00"
  },
  {
    "oldStatus": "SUBMITTED",
    "newStatus": "IN_REVIEW",
    "changedBy": "agent@gov.lk",
    "remarks": "Starting document verification.",
    "changedAt": "2026-06-22T09:30:00"
  }
]
```

> `oldStatus` is `null` for the initial SUBMITTED entry.

---

## HTTP Status Codes

| Code | Meaning                                                                      |
|------|------------------------------------------------------------------------------|
| 200  | OK — successful GET / PATCH                                                  |
| 201  | Created — resource created                                                   |
| 400  | Bad Request — invalid status transition, terminal-state mutation, or operation on a CANCELLED request |
| 401  | Unauthorized — missing/invalid/expired JWT, deactivated account, or session invalidated by new login |
| 403  | Forbidden — insufficient role or ownership violation                         |
| 404  | Not Found — resource not found                                               |
| 409  | Conflict — duplicate NIC, email, or mobile number                            |
| 422  | Unprocessable Entity — bean validation failure (password complexity, required fields) |
| 500  | Internal Server Error                                                        |

---

## Error Response Format

```json
{
  "timestamp": "2026-06-22T09:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Citizen with reference CIT-UNKNOWN not found",
  "path": "/api/v1/citizens/CIT-UNKNOWN"
}
```

Validation errors add `fieldErrors`:

```json
{
  "timestamp": "2026-06-22T09:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "path": "/api/v1/citizens",
  "fieldErrors": {
    "temporaryPassword": "Password must be at least 8 characters and contain an uppercase letter, a digit, and a special character",
    "mobileNumber": "must not be blank"
  }
}
```

---

## Default Seed Accounts

| Role          | Username           | Password      | Active | Notes |
|---------------|--------------------|---------------|--------|-------|
| ADMIN         | admin@gov.lk       | Admin@123     | Yes    | |
| SERVICE_AGENT | agent@gov.lk       | Agent@123     | Yes    | |
| CITIZEN       | citizen@gov.lk     | Citizen@123   | Yes    | `mustChangePassword = true`; linked to a pre-seeded citizen profile |
| CITIZEN       | inactive@gov.lk    | Inactive@123  | **No** | Permanently inactive; used to test deactivated-account login rejection |
