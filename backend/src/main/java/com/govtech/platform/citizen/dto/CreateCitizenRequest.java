package com.govtech.platform.citizen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new citizen profile.
 *
 * <p>The {@code email} is used as the {@code username} for the linked
 * {@link com.govtech.platform.auth.entity.User} login account.</p>
 *
 * <p>The {@code temporaryPassword} is BCrypt-encoded and stored against the User account.
 * It is <strong>never returned</strong> in any subsequent response. In production, it must
 * be delivered to the citizen through a secure out-of-band channel (email / SMS / identity
 * verification) and changed on first login ({@code mustChangePassword = true}).</p>
 */
@Schema(description = "Request payload for creating a new citizen profile and linked login account.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCitizenRequest {

    @Schema(description = "Full legal name of the citizen.", example = "Kamal Perera",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "National Identity Card number. Optional. If provided, must be unique and a valid Sri Lankan NIC: old format = 9 digits + V or X (10 chars total, e.g. 871840504V); new format = 12 digits only (e.g. 199012345678).",
            example = "871840504V",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^$|^\\d{9}[VvXx]$|^\\d{12}$",
             message = "NIC must be a valid Sri Lankan NIC: old format is 9 digits followed by V or X (e.g. 871840504V); new format is 12 digits only (e.g. 199012345678)")
    private String nic;

    @Schema(description = "Email address. Used as the citizen's login username. Must be unique.",
            example = "kamal.perera@email.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Schema(description = "Mobile phone number. Must be exactly 10 digits and unique across all citizens.",
            example = "0771234567",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    @Schema(description = "Residential address.", example = "123 Main Street, Colombo 03",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Address is required")
    private String address;

    @Schema(description = "Temporary password for the citizen's login account (minimum 8 characters). "
                        + "This password is BCrypt-encoded and stored — it is never returned in any response. "
                        + "Communicate it to the citizen securely out-of-band.",
            example = "TempPass@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Temporary password is required")
    @Size(min = 8, message = "Temporary password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Temporary password must contain at least one uppercase letter, one digit, and one special character"
    )
    private String temporaryPassword;
}
