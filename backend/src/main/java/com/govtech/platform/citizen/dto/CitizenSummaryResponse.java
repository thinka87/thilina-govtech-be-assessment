package com.govtech.platform.citizen.dto;

import com.govtech.platform.common.enums.CitizenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lightweight citizen summary used in paginated list responses.
 *
 * <p>Contains only the fields needed for search result display. Full profile
 * details (address, linked user) are available via the individual GET endpoint.
 * Excludes {@code username} to avoid lazy-loading the {@code User} association
 * on every row in a paginated list.</p>
 */
@Schema(description = "Lightweight citizen summary for paginated list responses. "
                    + "Use GET /citizens/{citizenReference} for full profile details.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenSummaryResponse {

    @Schema(description = "Unique citizen reference.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "Full name of the citizen.", example = "Kamal Perera")
    private String name;

    @Schema(description = "National Identity Card number.", example = "199012345678")
    private String nic;

    @Schema(description = "Email address.", example = "kamal.perera@email.com")
    private String email;

    @Schema(description = "Mobile phone number.", example = "+94771234567")
    private String mobileNumber;

    @Schema(description = "Current citizen status.", example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private CitizenStatus status;
}
