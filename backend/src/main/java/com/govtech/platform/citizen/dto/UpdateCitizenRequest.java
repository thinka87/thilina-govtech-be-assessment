package com.govtech.platform.citizen.dto;

import com.govtech.platform.common.enums.CitizenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating an existing citizen profile.
 *
 * <p>NIC cannot be changed — it is a permanent government identifier.
 * If the email changes, the linked {@link com.govtech.platform.auth.entity.User}
 * username is also updated, invalidating any existing JWT tokens for that citizen
 * (they must re-login with the new email).</p>
 *
 * <p>{@code status} is optional. When {@code INACTIVE} is set, the linked User
 * account is also deactivated. Omitting it preserves the current status.</p>
 */
@Schema(description = "Request payload for updating a citizen profile. NIC cannot be changed.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCitizenRequest {

    @Schema(description = "Updated full name.", example = "Kamal Perera",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Updated email address. If changed, the linked login username is also updated "
                        + "and existing JWT tokens are invalidated.",
            example = "kamal.updated@email.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Schema(description = "Updated mobile phone number. Must be exactly 10 digits and unique across all citizens.",
            example = "0771234567",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    @Schema(description = "Updated residential address.", example = "456 New Road, Colombo 07",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Address is required")
    private String address;

    @Schema(description = "Optional status update. If INACTIVE, the linked login account is also disabled. "
                        + "Omit to preserve the current status.",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private CitizenStatus status;
}
