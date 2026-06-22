# Digital Government Service Request Platform

A full-stack web application that allows citizens to submit and track government service requests, and government agents and administrators to manage them. The platform consists of a Spring Boot REST API backend, a React + TypeScript frontend, and a PostgreSQL database — all containerised with Docker Compose.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
  - [Option A: Docker Compose (Recommended)](#option-a-docker-compose-recommended)
  - [Option B: Local Development](#option-b-local-development)
- [Default Users](#default-users)
- [Frontend](#frontend)
- [API Documentation](#api-documentation)
- [Postman Collection](#postman-collection)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [Error Response Format](#error-response-format)
- [Further Documentation](#further-documentation)

---

## Tech Stack

### Backend

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 17                             |
| Framework        | Spring Boot 3.2.5                   |
| Security         | Spring Security + JWT (jjwt 0.12.3) |
| Persistence      | Spring Data JPA + Hibernate         |
| Database         | PostgreSQL 16                       |
| Migrations       | Flyway                              |
| Build Tool       | Maven 3.x                           |
| API Docs         | SpringDoc OpenAPI 3 (Swagger UI)    |
| Utilities        | Lombok                              |
| Testing          | JUnit 5, Mockito, AssertJ           |

### Frontend

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | TypeScript                          |
| Framework        | React 18                            |
| Build Tool       | Vite                                |
| Styling          | Tailwind CSS                        |
| HTTP Client      | Axios                               |
| Routing          | React Router v6                     |
| Web Server       | Nginx (production container)        |

### Infrastructure

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Containerisation | Docker + Docker Compose             |
| Timezone         | Asia/Colombo (UTC+5:30)             |

---

## Architecture Overview

The system is composed of three Docker containers:

```
┌─────────────────────────────────────────────────────────┐
│  Browser                                                │
│  React + TypeScript SPA  →  http://localhost:3000       │
└────────────────────┬────────────────────────────────────┘
                     │ REST (Axios + Bearer JWT)
┌────────────────────▼────────────────────────────────────┐
│  Spring Boot API             http://localhost:8080/api  │
│  ├── auth            # Login, JWT issuance, password    │
│  ├── citizen         # Citizen profile management       │
│  ├── servicerequest  # Service request lifecycle        │
│  ├── document        # Supporting document metadata     │
│  ├── notification    # Status-change notifications      │
│  ├── statushistory   # Immutable audit trail            │
│  └── common          # Shared enums, exceptions, utils  │
└────────────────────┬────────────────────────────────────┘
                     │ JDBC
┌────────────────────▼────────────────────────────────────┐
│  PostgreSQL 16               port 5432                  │
└─────────────────────────────────────────────────────────┘
```

**Service request lifecycle:**

```
Citizen submits SR (SUBMITTED)
    → Agent reviews (IN_REVIEW)
        → Agent approves (APPROVED) or rejects (REJECTED)
Admin can cancel at SUBMITTED or IN_REVIEW → CANCELLED
```

---

## Prerequisites

- **Docker & Docker Compose** (for Option A)
  OR
- **Java 17+**, **Maven 3.8+**, **Node.js 18+**, **PostgreSQL 16** (for Option B)

---

## Running the Application

### Option A: Docker Compose (Recommended)

```bash
# From the repository root
docker compose up --build -d
```

This starts three containers:

| Container          | Description                        | Port  |
|--------------------|------------------------------------|-------|
| `govtech-postgres` | PostgreSQL 16 database             | 5432  |
| `govtech-backend`  | Spring Boot REST API               | 8080  |
| `govtech-frontend` | React SPA served via Nginx         | 3000  |

The backend waits for the database to be healthy before starting. On first run, Flyway creates all tables and `DataInitializer` seeds the default users.

Access the application:
- **Frontend UI**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`

To stop:
```bash
docker compose down
```

To stop and remove all data volumes (wipes the database):
```bash
docker compose down -v
```

### Option B: Local Development

**1. Start PostgreSQL:**

```bash
createdb govtech_db
```

**2. Set environment variables:**

```bash
export DB_URL=jdbc:postgresql://localhost:5432/govtech_db
export DB_USERNAME=govtech
export DB_PASSWORD=govtech123
export JWT_SECRET=your-256-bit-base64-encoded-secret-here
export JWT_EXPIRATION_MS=86400000
export TZ=Asia/Colombo
```

**3. Start the backend:**

```bash
cd backend
mvn spring-boot:run
```

**4. Start the frontend:**

```bash
cd frontend
npm install
npm run dev
```

The API will be available at `http://localhost:8080/api` and the frontend at `http://localhost:5173`.

---

## Default Users

The following accounts are seeded automatically on startup:

| Role          | Email           | Password    | Notes                                                         |
|---------------|-----------------|-------------|---------------------------------------------------------------|
| ADMIN         | admin@gov.lk    | Admin@123   | Full access — manage citizens, cancel requests                |
| SERVICE_AGENT | agent@gov.lk    | Agent@123   | Review, approve, and reject service requests                  |
| CITIZEN       | citizen@gov.lk  | Citizen@123 | Has a linked citizen profile — can submit and track requests. **Note:** If the Postman collection is run initially, the password is changed to `Citizen@12345` |

**Password complexity rules** (enforced on password change and citizen creation):
- Minimum 8 characters
- At least one uppercase letter (A–Z)
- At least one digit (0–9)
- At least one special character (e.g. `@`, `!`, `#`, `$`)

> **Single session enforcement:** each login invalidates all previously issued tokens for that user. Concurrent sessions are not permitted.

---

## Frontend

The React SPA is served from `http://localhost:3000` and provides role-specific views:

| Role          | Landing page         | Key features                                                         |
|---------------|----------------------|----------------------------------------------------------------------|
| ADMIN         | `/admin/dashboard`   | Citizen management (create, edit, deactivate), service request oversight, cancel requests |
| SERVICE_AGENT | `/agent/dashboard`   | Search and process service requests, view citizen info, verify documents |
| CITIZEN       | `/citizen/dashboard` | Submit requests, track status, upload document metadata, view notifications |

**Navigation is role-gated** — authenticated routes redirect unauthenticated users to `/login`, and role-based routes return HTTP 403 if accessed by a user with an insufficient role.

---

## API Documentation

### Swagger UI (Interactive)

```
http://localhost:8080/api/swagger-ui/index.html
```

### OpenAPI JSON Spec

```
http://localhost:8080/api/v3/api-docs
```

### Base URL

```
http://localhost:8080/api
```

### Authentication

All endpoints (except `POST /v1/auth/login`) require a Bearer token:

```
Authorization: Bearer <accessToken>
```

Obtain a token by calling:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin@gov.lk",
  "password": "Admin@123"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin@gov.lk",
  "role": "ADMIN",
  "mustChangePassword": false
}
```

### Endpoint Groups

| Group               | Base Path                                   | Roles                            |
|---------------------|---------------------------------------------|----------------------------------|
| Authentication      | `/v1/auth`                                  | Public / Any                     |
| Citizen Management  | `/v1/citizens`                              | ADMIN (write), ADMIN + SERVICE_AGENT (read) |
| Service Requests    | `/v1/service-requests`                      | CITIZEN / ADMIN / SERVICE_AGENT  |
| Service Types       | `/v1/service-requests/types`                | Any authenticated                |
| Documents           | `/v1/service-requests/{ref}/documents`, `/v1/documents` | CITIZEN / ADMIN / SERVICE_AGENT |
| Notifications       | `/v1/citizens/{ref}/notifications`          | CITIZEN                          |
| Status History      | `/v1/service-requests/{ref}/status-history` | ADMIN / SERVICE_AGENT            |

See [docs/API.md](docs/API.md) for the full endpoint reference.

---

## Postman Collection

A complete Postman collection with requests across 7 folders is included under `postman/`:

| File                                                                  | Purpose                              |
|-----------------------------------------------------------------------|--------------------------------------|
| `postman/Digital-Government-Service-Platform.postman_collection.json` | All API requests with test scripts   |
| `postman/local.postman_environment.json`                              | Local environment variables          |

See [docs/POSTMAN_GUIDE.md](docs/POSTMAN_GUIDE.md) for import and usage instructions.

---

## Running Tests

```bash
cd backend
mvn test
```

**20 tests** across 3 test classes:
- `CitizenServiceTest` — 6 unit tests (citizen creation and deactivation)
- `ServiceRequestServiceTest` — 13 unit tests (SR creation, status transitions, cancellation, ownership)
- `PlatformApplicationTests` — 1 Spring context smoke test

All tests use an in-memory H2 database and Mockito mocks — no external services required.

---

## Project Structure

```
govtech-assessment/
├── backend/                        # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/govtech/platform/
│   │   │   │   ├── auth/           # Authentication & JWT
│   │   │   │   ├── citizen/        # Citizen management
│   │   │   │   ├── servicerequest/ # Service requests
│   │   │   │   ├── document/       # Supporting documents
│   │   │   │   ├── notification/   # Notifications
│   │   │   │   ├── statushistory/  # Status audit trail
│   │   │   │   ├── common/         # Shared utilities
│   │   │   │   └── config/         # Security, OpenAPI config
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/   # Flyway SQL migrations (V1–V3)
│   │   └── test/                   # Unit tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                       # React + TypeScript SPA
│   ├── src/
│   │   ├── api/                    # Axios API clients and endpoint config
│   │   ├── auth/                   # AuthContext, ProtectedRoute, authService
│   │   ├── components/             # Reusable UI components (Button, Card, Badge…)
│   │   ├── pages/
│   │   │   ├── admin/              # Admin dashboard, citizen management
│   │   │   ├── agent/              # Agent dashboard, request processing
│   │   │   └── citizen/            # Citizen dashboard, requests, notifications
│   │   ├── routes/                 # AppRoutes (React Router)
│   │   ├── types/                  # TypeScript interfaces
│   │   └── utils/                  # Date formatting, error helpers
│   ├── Dockerfile
│   └── vite.config.ts
├── postman/                        # Postman collection & environment
├── docs/                           # Extended documentation
├── docker-compose.yml
└── README.md
```

---

## Environment Variables

### Backend

| Variable            | Default (local)                               | Description                    |
|---------------------|-----------------------------------------------|--------------------------------|
| `DB_URL`            | `jdbc:postgresql://localhost:5432/govtech_db` | JDBC connection URL            |
| `DB_USERNAME`       | `govtech`                                     | Database username              |
| `DB_PASSWORD`       | `govtech123`                                  | Database password              |
| `JWT_SECRET`        | *(required)*                                  | Base64-encoded 256-bit secret  |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours)                         | Token lifetime in milliseconds |
| `TZ`                | `Asia/Colombo`                                | JVM / container timezone       |
| `JAVA_TOOL_OPTIONS` | `-Duser.timezone=Asia/Colombo`                | JVM timezone override          |

### Database

| Variable      | Value           | Description             |
|---------------|-----------------|-------------------------|
| `TZ`          | `Asia/Colombo`  | Container timezone      |
| `PGTZ`        | `Asia/Colombo`  | PostgreSQL session timezone |

---

## Error Response Format

All errors return a consistent JSON body:

```json
{
  "timestamp": "2026-06-22T09:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable description of the error",
  "path": "/api/v1/..."
}
```

Validation errors (HTTP 422) add a `fieldErrors` map:

```json
{
  "timestamp": "2026-06-22T09:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "path": "/api/v1/...",
  "fieldErrors": {
    "temporaryPassword": "Password must be at least 8 characters and contain an uppercase letter, a digit, and a special character"
  }
}
```

---

## Further Documentation

| Document                                           | Description                                        |
|----------------------------------------------------|----------------------------------------------------|
| [docs/API.md](docs/API.md)                         | Full API endpoint reference                        |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | Database schema and design decisions               |
| [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md)         | Assumptions made during development                |
| [docs/SECURITY.md](docs/SECURITY.md)               | Security design, JWT, RBAC, and improvement areas  |
| [docs/POSTMAN_GUIDE.md](docs/POSTMAN_GUIDE.md)     | How to import and run the Postman collection       |
