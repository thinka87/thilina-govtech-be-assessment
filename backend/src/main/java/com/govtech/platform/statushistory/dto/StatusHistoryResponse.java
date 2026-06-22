package com.govtech.platform.statushistory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit record of a service request status transition.
 *
 * <p>Returned by the GET {@code /service-requests/{ref}/status-history} endpoint.
 * Records are ordered newest-first to surface the most recent transition at the top.</p>
 *
 * <p>{@code oldStatus} is {@code null} for the initial {@code SUBMITTED} entry
 * (there was no previous state). Consumers should handle {@code null} accordingly.</p>
 */
@Schema(description = "Immutable audit record of a single service request status transition. "
                    + "Records are ordered newest-first. oldStatus is null for the initial SUBMITTED entry.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryResponse {

    @Schema(description = "Database primary key of this history record.", example = "7")
    private Long id;

    @Schema(description = "The service request this record belongs to.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "The status before this transition. Null for the initial SUBMITTED entry.",
            example = "SUBMITTED",
            nullable = true)
    private ServiceRequestStatus oldStatus;

    @Schema(description = "The status after this transition.", example = "IN_REVIEW")
    private ServiceRequestStatus newStatus;

    @Schema(description = "Username of the actor who performed the status change.", example = "agent@gov.lk")
    private String changedBy;

    @Schema(description = "Optional notes explaining the reason for the transition.",
            example = "Initial review started. All documents appear complete.")
    private String remarks;

    @Schema(description = "Immutable timestamp of this transition.", example = "2026-06-21T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
}
