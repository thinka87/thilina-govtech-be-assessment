package com.govtech.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for the POST /v1/auth/login endpoint.
 *
 * <p>For citizen accounts, {@code username} is the citizen's email address.</p>
 */
@Schema(description = "Login credentials. Use email as username for citizen accounts.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "Login username. For citizens: their email address. For admin/agent: their registered email.",
            example = "admin@gov.lk",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(description = "Account password.",
            example = "Admin@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;
}
