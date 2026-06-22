package com.govtech.platform.citizen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.CitizenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response returned immediately after a citizen profile is created.
 *
 * <p>This response is the only opportunity to communicate the temporary password
 * context. The encoded password is <strong>never</strong> included; only the
 * {@code temporaryPasswordNote} and {@code mustChangePassword} flag are present.</p>
 *
 * <p><strong>In production:</strong> the admin should communicate the temporary
 * password to the citizen via a secure channel (email / SMS / identity verification)
 * before returning it in an API response visible to intermediaries.</p>
 */
@Schema(description = "Response returned immediately after a citizen profile is successfully created.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenCreatedResponse {

    @Schema(description = "System-generated unique citizen reference.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "Full name of the citizen.", example = "Kamal Perera")
    private String name;

    @Schema(description = "National Identity Card number.", example = "199012345678")
    private String nic;

    @Schema(description = "Email address (also the login username).", example = "kamal.perera@email.com")
    private String email;

    @Schema(description = "Mobile phone number.", example = "+94771234567")
    private String mobileNumber;

    @Schema(description = "Residential address.", example = "123 Main Street, Colombo 03")
    private String address;

    @Schema(description = "Initial citizen status, always ACTIVE on creation.", example = "ACTIVE")
    private CitizenStatus status;

    @Schema(description = "Login username of the created User account (same as email).",
            example = "kamal.perera@email.com")
    private String username;

    @Schema(description = "Always true for newly created citizen accounts — the citizen must change their temporary password.",
            example = "true")
    private boolean mustChangePassword;

    @Schema(description = "Advisory note about the temporary password. The plaintext password is never included in any response.",
            example = "Temporary password was set during citizen creation. In production, deliver via secure channel and require change after first login.")
    private String temporaryPasswordNote;

    @Schema(description = "Profile creation timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
