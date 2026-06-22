package com.govtech.platform.servicerequest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.document.dto.SupportingDocumentSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight service request summary used in paginated list responses.
 *
 * <p>Contains only the fields needed for search result display. Full details
 * (description, citizen name) are available via the individual GET endpoint.
 * Excludes {@code description} and {@code citizenName} to minimise payload size
 * when listing many requests.</p>
 */
@Schema(description = "Lightweight service request summary for paginated list responses. "
                    + "Use GET /service-requests/{requestReference} for full details.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestSummaryResponse {

    @Schema(description = "Unique request reference.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "Service type category.", example = "PASSPORT_RENEWAL")
    private String serviceType;

    @Schema(description = "Current processing status.", example = "SUBMITTED",
            allowableValues = {"SUBMITTED", "IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED"})
    private ServiceRequestStatus status;

    @Schema(description = "Reference of the citizen who submitted this request.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "Request creation timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Supporting documents attached to this request. Only populated for citizen-scoped endpoints.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SupportingDocumentSummaryResponse> documents;
}
