# Security Design

---

## Overview

The platform uses a stateless, token-based security model built on **Spring Security** and **JSON Web Tokens (JWT)**. Access control is role-based and enforced at the method level.

---

## Authentication

### JWT Issuance

On a successful `POST /v1/auth/login`, the server:

1. Loads the user from the database by username (email).
2. Verifies the submitted password against the BCrypt hash stored in `users.password`.
3. Confirms the user account is active (`users.active = true`).
4. Increments `users.token_version` and persists the new value.
5. Signs and returns a JWT containing the username, role, and current `token_version` as claims.

The JWT is signed using **HMAC-SHA256** (`HS256`) with a secret key supplied via the `JWT_SECRET` environment variable.

### JWT Validation — Request Filter

`JwtAuthenticationFilter` intercepts every request:

1. Extracts the `Authorization: Bearer <token>` header.
2. Validates the token signature and expiry using the same secret key.
3. Extracts the username from the token claims.
4. Loads the user from the database via `CustomUserDetailsService`, which returns a `CustomUserDetails` object carrying the user's current `token_version`.
5. Checks `userDetails.isEnabled()` — if the account has been deactivated, the request is rejected with `401 Unauthorized` immediately, even if the token signature is valid.
6. Compares the `ver` claim in the token against the user's current `token_version` in the database. If they differ (i.e. the user has logged in again on another device), the request is rejected with `401 Unauthorized`.
7. Populates the Spring Security context with the authenticated principal and their granted authority (`ROLE_<ROLE_NAME>`).

This means deactivated accounts and stale sessions from previous logins are blocked on every request, not just at login time.

### Single Session Enforcement

Each login increments `users.token_version` and embeds the new version as the `ver` claim in the issued JWT. The filter validates this claim on every request. As a result:

- If a user logs in from a second device, the first device's token version no longer matches and all its requests receive `401 Unauthorized`.
- Changing password also calls the login flow, which increments the version and invalidates all existing sessions.
- Only one active session per user is permitted at any time.

### Token Lifetime

Default: **24 hours** (configurable via `JWT_EXPIRATION_MS`). No refresh token mechanism is provided; users must re-authenticate after expiry.

### Password Hashing

All passwords are hashed with **BCrypt** via Spring Security's `PasswordEncoder`. Plaintext passwords are never stored or logged.

---

## Authorisation

### Role-Based Access Control (RBAC)

Three roles are defined:

| Role            | Description                                          |
|-----------------|------------------------------------------------------|
| `ADMIN`         | Full administrative access; manages citizens; can cancel requests |
| `SERVICE_AGENT` | Reviews, approves, and rejects service requests; verifies documents |
| `CITIZEN`       | Submits service requests; uploads documents; views own notifications |

Method-level security is enabled via `@EnableMethodSecurity` on `SecurityConfig`. Endpoints are protected with `@PreAuthorize` annotations, for example:

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_AGENT')")
@PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
```

### Public Endpoints

The following paths are accessible without authentication:

- `POST /v1/auth/login`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`

All other endpoints require a valid JWT.

---

## Ownership Validation

Role checks alone are insufficient for citizen-scoped operations. The service layer additionally verifies resource ownership:

- **Service requests:** When a citizen accesses `GET /v1/service-requests/citizen/{citizenRef}`, the service compares the authenticated user's username against `citizen.user.username`. If they do not match, `UnauthorizedActionException` (HTTP 403) is thrown.
- **Documents:** Citizens can only add, view, update documents linked to their own service requests.
- **Notifications:** Citizens can only read and mark notifications that belong to their own citizen profile.

Admins and service agents bypass ownership checks and can access all resources.

---

## HTTP Response Codes for Security Errors

| Scenario                                           | HTTP Status      |
|----------------------------------------------------|------------------|
| Missing or malformed `Authorization` header        | 401 Unauthorized |
| Invalid or expired JWT                             | 401 Unauthorized |
| Valid token, account deactivated                   | 401 Unauthorized |
| Valid token, session invalidated (new login issued)| 401 Unauthorized |
| Valid token, insufficient role                     | 403 Forbidden    |
| Valid token, correct role, wrong owner             | 403 Forbidden    |

---

## Input Validation

All incoming request bodies are validated with **Jakarta Bean Validation** (`@Valid`). Field-level errors are returned with HTTP 422 and a `fieldErrors` map. This prevents malformed data from reaching the service layer.

SQL injection is mitigated by using **Spring Data JPA with parameterised queries** — no raw SQL string concatenation is used in the application code.

---

## Stateless Sessions

The Spring Security session creation policy is set to `STATELESS`. The server creates no HTTP session and no session cookie. Every request must carry its own JWT. This makes the application horizontally scalable and eliminates session fixation attacks.

**CSRF protection is disabled** — it is not applicable to stateless JWT APIs that do not rely on cookies for authentication.

---

## Docker Security

The Docker runtime image (`eclipse-temurin:17-jre-alpine`) uses a dedicated non-root OS user (`govtech`) created during the image build. The application process never runs as root inside the container.

JVM flags applied at startup:
- `-XX:+UseContainerSupport` — respects container CPU/memory limits
- `-XX:MaxRAMPercentage=75.0` — caps heap at 75% of container memory
- `-Djava.security.egd=file:/dev/./urandom` — faster secure random seeding on Linux

---

## Recommended Improvements for Production

The following items are out of scope for this assessment but would be required before a production deployment:

1. **HTTPS / TLS termination.** Place a reverse proxy (e.g., Nginx, AWS ALB) in front of the application with a valid TLS certificate. All HTTP traffic should be redirected to HTTPS.

2. **Token revocation / explicit logout.** The current single-session model invalidates old sessions on new login, but there is no explicit logout endpoint that invalidates the current token. A short-lived blocklist (e.g., Redis) would allow immediate revocation on logout.

3. **Refresh tokens.** Issue short-lived access tokens (e.g., 15 minutes) and long-lived refresh tokens to balance security and usability.

4. **Rate limiting.** Apply rate limiting on `POST /v1/auth/login` to prevent brute-force password attacks.

5. **Audit logging.** Log all authentication events and sensitive data changes to an immutable audit store.

6. **CORS policy.** Configure an explicit `CorsConfigurationSource` to allow only the frontend origin.

7. **Secrets management.** Store `JWT_SECRET` and database credentials in a secrets manager (e.g., AWS Secrets Manager, HashiCorp Vault) rather than environment variables.

8. **Dependency scanning.** Integrate OWASP Dependency-Check or Snyk into the CI pipeline to detect vulnerable library versions.
