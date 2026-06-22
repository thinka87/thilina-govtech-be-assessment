# Postman Guide

This guide explains how to import and run the included Postman collection to test the Digital Government Service Request Platform API end-to-end.

---

## Files

| File | Location |
|------|----------|
| Collection | `postman/Digital-Government-Service-Platform.postman_collection.json` |
| Environment | `postman/local.postman_environment.json` |

---

## Import Instructions

1. Open **Postman**.
2. Click **Import** (top-left).
3. Drag and drop both files, or click **Upload Files** and select them.
4. After import you will see:
   - A collection named **Digital Government Service Platform** in the left sidebar.
   - An environment named **GovTech Local Environment** in the Environments list.
5. Select **GovTech Local Environment** from the environment dropdown (top-right corner).

---

## Prerequisites

- The full stack must be running: `docker compose up --build -d`
- Run against a **fresh database** (`docker compose down -v` first) to ensure seeded credentials are in their initial state.
- See the main [README.md](../README.md) for startup instructions.

---

## Seeded Accounts

The application seeds the following accounts on every startup. These are used directly by the Postman environment — no manual account creation is required before running the collection.

| Username | Password | Role | Active | Notes |
|---|---|---|---|---|
| `admin@gov.lk` | `Admin@123` | ADMIN | Yes | |
| `agent@gov.lk` | `Agent@123` | SERVICE_AGENT | Yes | |
| `citizen@gov.lk` | `Citizen@123` | CITIZEN | Yes | `mustChangePassword = true` on first login |
| `inactive@gov.lk` | `Inactive@123` | CITIZEN | **No** | Used by the deactivated-account error test |

The `citizen@gov.lk` account is also linked to a pre-seeded **Citizen profile** (`Test Citizen`), so it can submit service requests immediately without requiring a separate Create Citizen step.

---

## Environment Variables

| Variable | Initial Value | Set Automatically By |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | — |
| `adminUsername` | `admin@gov.lk` | — |
| `adminPassword` | `Admin@123` | — |
| `agentUsername` | `agent@gov.lk` | — |
| `agentPassword` | `Agent@123` | — |
| `citizenUsername` | `citizen@gov.lk` | — |
| `citizenPassword` | `Citizen@123` | Updated to `citizenNewPassword` after **Change Citizen Password** runs |
| `citizenNewPassword` | `Citizen@12345` | — |
| `deactivatedUsername` | `inactive@gov.lk` | — |
| `deactivatedPassword` | `Inactive@123` | — |
| `adminToken` | *(empty)* | Login as Admin |
| `agentToken` | *(empty)* | Login as Service Agent |
| `citizenToken` | *(empty)* | Login as Citizen |
| `citizenReference` | *(empty)* | Login as Citizen (and Create Service Request) |
| `requestReference` | *(empty)* | Create Service Request; overwritten by section 04 setup step |
| `documentReference` | *(empty)* | Add Document Before Cancel (Setup); overwritten by section 04 Add Document |
| `notificationId` | *(empty)* | View Citizen Notifications |
| `approvalRequestRef` | *(empty)* | 08/1 Create Service Request for Approval |
| `rejectionRequestRef` | *(empty)* | 08/5 Create Service Request for Rejection |

---

## Collection Structure

The collection is organised into 8 folders containing **57 requests** in total.

| Folder | Requests | Purpose |
|---|---|---|
| 01 - Authentication | 5 | Login, password change, deactivated-account error |
| 02 - Citizen Management | 7 | Admin creates and manages citizens |
| 03 - Service Request Management | 13 | Create, view, update, cancel requests; cancelled-state error tests |
| 04 - Supporting Document Management | 9 | Add and manage document metadata; document error tests |
| 05 - Notification and Status History | 5 | View notifications and audit trail |
| 06 - Security and Common Error Scenarios | 8 | Verify 401, 403, and 404 behaviours |
| 07 - Service Types | 2 | Verify the controlled service-type list endpoint |
| 08 - Approval and Rejection Flow | 8 | Full APPROVED and REJECTED terminal-state flows |

---

## Running as a Collection (Recommended)

The collection is fully self-contained and designed to run **top-to-bottom with no manual steps**.

1. Click the **···** (three dots) next to the collection name.
2. Select **Run collection**.
3. Ensure **GovTech Local Environment** is selected.
4. Keep the default request order.
5. Click **Run Digital Government Service Platform**.

All 87 assertions will be evaluated and a pass/fail report will be shown.

> **Important:** Always run against a fresh database. Run `docker compose down -v && docker compose up --build -d` before each collection run to reset seeded passwords and data.

---

## Request-by-Request Walkthrough

### 01 — Authentication

| # | Request | What Happens |
|---|---|---|
| 1 | Login as Admin | Saves `adminToken` |
| 2 | Login as Service Agent | Saves `agentToken` |
| 3 | Login as Citizen | Logs in `citizen@gov.lk`; saves `citizenToken` and `citizenReference` |
| 4 | Change Citizen Password | Changes password from `citizenPassword` to `citizenNewPassword`; updates `citizenPassword` env var |
| 5 | Login - Deactivated Account Error | Attempts login as `inactive@gov.lk`; expects **401** |

### 02 — Citizen Management

| # | Request | What Happens |
|---|---|---|
| 1 | Create Citizen | Admin creates Kamal Perera with a temporary password |
| 2 | View Citizen by Reference | Admin reads back the created citizen |
| 3 | List / Search Citizens | Admin lists all citizens |
| 4 | Update Citizen | Admin updates citizen contact details |
| 5 | Deactivate Citizen | Admin soft-deletes the citizen (sets status = INACTIVE) |
| 6 | Duplicate Citizen NIC Error | Expects **409** — NIC already exists |
| 7 | Access Create Citizen as Citizen - Forbidden | Expects **403** — citizens cannot create citizens |

### 03 — Service Request Management

| # | Request | What Happens |
|---|---|---|
| 1 | Create Service Request as Citizen | Citizen submits a request; saves `requestReference` and `citizenReference` |
| 2 | View My Service Requests | Citizen views their own requests |
| 3 | Search Service Requests as Agent | Agent searches all requests |
| 4 | View Service Request Details as Agent | Agent reads the specific request |
| 5 | Update Service Request Details as Agent | Agent updates description |
| 6 | Update Service Request Status as Agent (SUBMITTED→IN_REVIEW) | Agent begins review |
| 7 | Invalid Status Update Error | Agent attempts IN_REVIEW→SUBMITTED; expects **400** |
| 8 | Add Document Before Cancel (Setup) | Citizen adds a document; saves `documentReference` — required so step 11 has a valid reference after the cancel |
| 9 | Cancel Service Request as Admin | Admin cancels the request (terminal state) |
| 10 | Add Document to Cancelled Request Error | Citizen attempts to add a document to the cancelled request; expects **400** |
| 11 | Verify Document on Cancelled Request Error | Agent attempts to verify a document on the cancelled request; expects **400** |
| 12 | Create Request with Invalid Citizen Error | Admin creates a request for `CIT-NOTEXIST`; expects **404** |
| 13 | Citizen Access Another Citizen Request Error | Citizen queries `CIT-NOTEXIST` requests; expects **404** |

### 04 — Supporting Document Management

| # | Request | What Happens |
|---|---|---|
| 0 | Create Service Request for Document Tests | Creates a fresh SUBMITTED request; overwrites `requestReference` — needed because the section 03 request was cancelled |
| 1 | Add Supporting Document as Citizen | Citizen attaches document metadata; overwrites `documentReference` |
| 2 | View Documents by Service Request as Agent | Agent lists documents on the request |
| 3 | View Document by Reference as Agent | Agent reads one document |
| 4 | Update Document Metadata as Agent | Agent updates document name/type |
| 5 | Update Document Verification Status as Agent | Agent marks document as VERIFIED |
| 6 | Delete Document as Admin | Admin hard-deletes the document record |
| 7 | Add Document to Invalid Request Error | Citizen adds a document to `REQ-NOTEXIST`; expects **404** |
| 8 | Citizen Add Document to Another Citizen Request Error | Citizen adds a document to `REQ-ANOTHER1`; expects **404** |

### 05 — Notification and Status History

| # | Request | What Happens |
|---|---|---|
| 1 | View Citizen Notifications | Citizen lists notifications; saves `notificationId` |
| 2 | Mark Notification as Read | Citizen marks the notification as READ |
| 3 | View Request Status History as Agent | Agent reads the full audit trail |
| 4 | Citizen View Another Citizen Notifications Error | Expects **403** |
| 5 | Citizen View Status History - Forbidden | Expects **403** |

### 06 — Security and Error Scenarios

| # | Request | Expected Response |
|---|---|---|
| 1 | Access Protected API Without Token | **401** Unauthorized |
| 2 | Access Protected API With Invalid Token | **401** Unauthorized |
| 3 | Access Admin API as Service Agent | **403** Forbidden |
| 4 | Access Agent API as Citizen | **403** Forbidden |
| 5 | View Non-existing Citizen | **404** Not Found |
| 6 | View Non-existing Service Request | **404** Not Found |
| 7 | View Non-existing Document | **404** Not Found |
| 8 | Mark Non-existing Notification as Read | **404** Not Found |

### 08 — Approval and Rejection Flow

Two independent sub-flows, each creating their own service request so they can run standalone after authentication tokens are available.

**Approval sub-flow**

| # | Request | What Happens |
|---|---|---|
| 1 | Create Service Request for Approval | Citizen creates a `PASSPORT_RENEWAL` request; saves `approvalRequestRef` |
| 2 | Move to IN_REVIEW (Approval Path) | Agent moves request to `IN_REVIEW` |
| 3 | Approve Service Request | Agent moves request to `APPROVED`; verifies terminal status |
| 4 | Attempt Transition on Approved Request Error | Agent tries `APPROVED → IN_REVIEW`; expects **400** |

**Rejection sub-flow**

| # | Request | What Happens |
|---|---|---|
| 5 | Create Service Request for Rejection | Citizen creates a `BIRTH_CERTIFICATE` request; saves `rejectionRequestRef` |
| 6 | Move to IN_REVIEW (Rejection Path) | Agent moves request to `IN_REVIEW` |
| 7 | Reject Service Request | Agent moves request to `REJECTED`; verifies terminal status |
| 8 | Attempt Transition on Rejected Request Error | Agent tries `REJECTED → IN_REVIEW`; expects **400** |

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| 401 on all requests | Token variables are empty | Run Login requests in folder 01 first |
| Login as Citizen returns 401 | Password was already changed in a previous run | Run `docker compose down -v && docker compose up --build -d` to reset the database |
| 403 on service request endpoints | `citizenReference` does not match the logged-in citizen | Ensure Login as Citizen (01/3) has run so `citizenReference` is saved from the login response |
| Section 04 tests return 400 "cancelled" | `requestReference` still points to the cancelled request | Section 04/0 "Create Service Request for Document Tests" must run first to reset `requestReference` |
| 400 on status update | Invalid status transition attempted | Check current request status and use an allowed transition |
| Connection refused | Backend not running | Run `docker compose up --build -d` |
