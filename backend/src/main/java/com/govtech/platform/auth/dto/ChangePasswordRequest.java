package com.govtech.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for the PATCH /v1/auth/change-password endpoint.
 *
 * <p>The {@code currentPassword} is verified against the stored BCrypt hash
 * before the new password is accepted. This prevents unauthorised password
 * changes even if a token is stolen.</p>
 */
@Schema(description = "Payload for changing the authenticated user's password.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @Schema(description = "The user's current password, used for verification before accepting the new password.",
            example = "Admin@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "The new password. Must be at least 8 characters. Cannot be the same as the current password.",
            example = "NewSecure@456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
        message = "New password must contain at least one uppercase letter, one digit, and one special character"
    )
    private String newPassword;
}
