package com.govtech.platform.servicerequest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response returned after a successful service request status transition.
 *
 * <p>Captures the before/after snapshot so the client can confirm the exact
 * transition that occurred without needing to re-fetch the full resource.</p>
 */
@Schema(description = "Response confirming a service request status transition, showing before and after states.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateResponse {

    @Schema(description = "The service request that was updated.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "The status before this update.", example = "SUBMITTED")
    private ServiceRequestStatus previousStatus;

    @Schema(description = "The status after this update.", example = "IN_REVIEW")
    private ServiceRequestStatus newStatus;

    @Schema(description = "Optional remarks provided by the agent at the time of the transition.",
            example = "Initial review started. Documents look complete.")
    private String remarks;

    @Schema(description = "Timestamp of the status change.", example = "2026-06-21T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
