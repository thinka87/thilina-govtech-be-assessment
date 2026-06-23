package com.govtech.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration for the Digital Government Service Request Platform.
 *
 * <h2>Accessing Swagger UI</h2>
 * <ol>
 *   <li>Start the application.</li>
 *   <li>Navigate to {@code http://localhost:8080/api/swagger-ui/index.html}</li>
 *   <li>Use {@code POST /api/v1/auth/login} to obtain a JWT token.</li>
 *   <li>Click <strong>Authorize</strong> (lock icon, top-right) and paste your JWT token
 *       (Swagger UI adds the "Bearer " prefix automatically).</li>
 *   <li>All subsequent Swagger requests will include the Authorization header.</li>
 * </ol>
 *
 * <h2>API Groups (Tags)</h2>
 * <ul>
 *   <li><strong>Authentication</strong>       — Login, change password</li>
 *   <li><strong>Citizen Management</strong>   — Admin CRUD for citizen profiles</li>
 *   <li><strong>Service Request Management</strong> — Create, search, process requests</li>
 *   <li><strong>Citizen Self-Service</strong> — Citizens view own service requests</li>
 *   <li><strong>Supporting Document Management</strong> — Document metadata CRUD</li>
 *   <li><strong>Notifications</strong>        — Citizen notification read/mark-read</li>
 *   <li><strong>Status History</strong>       — Immutable audit trail per request</li>
 * </ul>
 *
 * <h2>Authentication</h2>
 * <p>All endpoints except {@code POST /v1/auth/login} and the Swagger/OpenAPI docs
 * require a JWT Bearer token. Use the Authorize button after logging in.</p>
 *
 * <h2>Import into Postman</h2>
 * <ol>
 *   <li>In Postman, click <strong>Import → Link</strong>.</li>
 *   <li>Enter {@code http://localhost:8080/api/v3/api-docs}.</li>
 *   <li>Set the collection-level auth to <strong>Bearer Token</strong> and paste your JWT.</li>
 * </ol>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Digital Government Service Request Platform API",
                version     = "1.0.0",
                description = "Secure REST API for managing digital government service requests, citizen profiles, "
                            + "supporting document metadata, in-app notifications, and audit status history. "
                            + "\n\n"
                            + "**How to authenticate:** "
                            + "Call `POST /v1/auth/login` with valid credentials to receive a JWT token. "
                            + "Click the **Authorize** button (🔓) at the top of this page, "
                            + "paste your JWT token (Swagger UI adds the `Bearer ` prefix automatically), and click Authorize. "
                            + "All subsequent requests will include the token automatically."
                            + "\n\n"
                            + "**Default test accounts (seeded on startup):**\n"
                            + "- Admin: `admin@gov.lk` / `Admin@123`\n"
                            + "- Service Agent: `agent@gov.lk` / `Agent@123`\n"
                            + "- Citizen (with profile): `citizen@gov.lk` / `Citizen@123` _(changes to `Citizen@12345` after running the Postman collection)_",
                contact = @Contact(name = "GovTech Platform Team")
        ),
        servers = @Server(url = "/api", description = "Local development server (http://localhost:8080/api)")
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER,
        description  = "JWT Bearer token. Obtain from POST /v1/auth/login. "
                     + "Paste the raw token here — Swagger UI adds the 'Bearer ' prefix automatically."
)
public class OpenApiConfig {
    // Configuration is fully annotation-driven — no bean methods required.
}
