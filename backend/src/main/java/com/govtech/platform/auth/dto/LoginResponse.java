package com.govtech.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response payload for successful authentication.
 *
 * <p>The {@code mustChangePassword} flag signals to the frontend that the user
 * (typically a newly created citizen) must update their temporary password before
 * accessing other features. The frontend should redirect to a change-password screen.</p>
 *
 * <p>The {@code accessToken} must be sent in the {@code Authorization: Bearer <token>}
 * header on all subsequent authenticated requests.</p>
 */
@Schema(description = "Successful login response containing the JWT Bearer token and user context.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "JWT Bearer token. Include in the Authorization header: 'Bearer <token>'.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBnb3YubGsifQ.abc123")
    private String accessToken;

    @Schema(description = "Token type. Always 'Bearer'.", example = "Bearer")
    private String tokenType;

    @Schema(description = "The authenticated user's username.", example = "admin@gov.lk")
    private String username;

    @Schema(description = "The user's role. One of: ADMIN, SERVICE_AGENT, CITIZEN.",
            example = "ADMIN",
            allowableValues = {"ADMIN", "SERVICE_AGENT", "CITIZEN"})
    private String role;

    @Schema(description = "True if the user must change their temporary password before using the platform. "
                        + "Applies to citizen accounts created by an admin.",
            example = "false")
    private boolean mustChangePassword;

    @Schema(description = "Citizen reference for CITIZEN role accounts. Null for ADMIN and SERVICE_AGENT.",
            example = "CIT-8F3A91B2")
    private String citizenReference;
}
