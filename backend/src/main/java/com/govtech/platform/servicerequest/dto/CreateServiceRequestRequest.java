package com.govtech.platform.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new service request.
 *
 * <p><strong>CITIZEN callers:</strong> {@code citizenReference} must be omitted or null —
 * the system derives it automatically from the authenticated JWT principal.
 * Any supplied value is ignored.</p>
 *
 * <p><strong>ADMIN callers:</strong> {@code citizenReference} is required to specify
 * which citizen the request is being created on behalf of.</p>
 */
@Schema(description = "Request payload for creating a new service request. "
                    + "CITIZEN: omit citizenReference (derived from JWT). "
                    + "ADMIN: citizenReference is required.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequestRequest {

    @Schema(description = "Category of the government service being requested.",
            example = "PASSPORT_RENEWAL",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Service type is required")
    private String serviceType;

    @Schema(description = "Detailed description of the service request and reason.",
            example = "Applying for passport renewal due to expiry. Current passport number: N1234567.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(description = "Target citizen reference. Required for ADMIN callers; ignored for CITIZEN callers (derived from JWT).",
            example = "CIT-8F3A91B2")
    private String citizenReference;
}
