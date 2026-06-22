package com.govtech.platform.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the details of an existing service request.
 *
 * <p>Only the {@code serviceType} and {@code description} can be changed.
 * Updates are only permitted while the request is in {@code SUBMITTED} status.
 * Once a request is {@code IN_REVIEW}, {@code APPROVED}, {@code REJECTED}, or
 * {@code CANCELLED} the content is locked.</p>
 *
 * <p>Access: <strong>SERVICE_AGENT only</strong></p>
 */
@Schema(description = "Request payload for updating service type and description. "
                    + "Only allowed while status is SUBMITTED.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequestRequest {

    @Schema(description = "Updated service type category.",
            example = "DRIVING_LICENSE_RENEWAL",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Service type is required")
    private String serviceType;

    @Schema(description = "Updated description of the service request.",
            example = "Correcting service type — applicant requires driving license renewal, not passport.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description is required")
    private String description;
}
