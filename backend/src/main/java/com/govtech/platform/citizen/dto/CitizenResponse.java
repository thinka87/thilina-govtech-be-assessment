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
 * Full citizen profile response returned for GET and PUT endpoints.
 *
 * <p>Password is intentionally excluded. The {@code username} field is included
 * to help admins verify which User account is linked to this profile.</p>
 */
@Schema(description = "Full citizen profile including linked login account username.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenResponse {

    @Schema(description = "Unique system-generated citizen reference.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "Full name of the citizen.", example = "Kamal Perera")
    private String name;

    @Schema(description = "National Identity Card number.", example = "199012345678")
    private String nic;

    @Schema(description = "Email address and login username.", example = "kamal.perera@email.com")
    private String email;

    @Schema(description = "Mobile phone number.", example = "+94771234567")
    private String mobileNumber;

    @Schema(description = "Residential address.", example = "123 Main Street, Colombo 03")
    private String address;

    @Schema(description = "Current status of the citizen profile.", example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private CitizenStatus status;

    @Schema(description = "Login username of the linked User account (same as email for citizens).",
            example = "kamal.perera@email.com")
    private String username;

    @Schema(description = "True if the citizen must change their temporary password before using the platform.",
            example = "true")
    private boolean mustChangePassword;

    @Schema(description = "Profile creation timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Profile last-updated timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
