package com.govtech.platform.servicerequest.dto;

import com.govtech.platform.common.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for advancing a service request to a new status.
 *
 * <p>Only the following transitions are permitted:</p>
 * <ul>
 *   <li>{@code SUBMITTED}  → {@code IN_REVIEW}  (agent picks up the request)</li>
 *   <li>{@code SUBMITTED}  → {@code CANCELLED}  (admin cancels before review)</li>
 *   <li>{@code IN_REVIEW}  → {@code APPROVED}   (agent approves)</li>
 *   <li>{@code IN_REVIEW}  → {@code REJECTED}   (agent rejects)</li>
 *   <li>{@code IN_REVIEW}  → {@code CANCELLED}  (admin cancels during review)</li>
 * </ul>
 *
 * <p>{@code APPROVED}, {@code REJECTED}, and {@code CANCELLED} are terminal states.
 * Attempting to transition from a terminal state throws {@code 400 Bad Request}.</p>
 *
 * <p>Access: <strong>SERVICE_AGENT only</strong></p>
 */
@Schema(description = "Request payload for transitioning a service request to a new status. "
                    + "Valid transitions: SUBMITTED→IN_REVIEW, IN_REVIEW→APPROVED|REJECTED. "
                    + "APPROVED/REJECTED/CANCELLED are terminal states.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequestStatusRequest {

    @Schema(description = "The target status. Must be a valid forward transition from the current status.",
            example = "IN_REVIEW",
            allowableValues = {"IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Status is required")
    private ServiceRequestStatus status;

    @Schema(description = "Optional explanation for the decision. Recommended when rejecting.",
            example = "Supporting documents are incomplete. Please upload a valid NIC copy.")
    private String remarks;
}
