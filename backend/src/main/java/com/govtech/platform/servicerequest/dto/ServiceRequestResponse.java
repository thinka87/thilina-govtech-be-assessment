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
 * Full service request profile returned for GET and PUT endpoints.
 *
 * <p>Includes citizen identification fields so consumers can display contextual
 * information without a separate citizen lookup. Password and sensitive citizen
 * data are intentionally excluded.</p>
 */
@Schema(description = "Full service request profile including citizen context.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestResponse {

    @Schema(description = "Unique system-generated request reference.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "Category of the government service.", example = "PASSPORT_RENEWAL")
    private String serviceType;

    @Schema(description = "Detailed description of the service request.",
            example = "Applying for passport renewal due to expiry.")
    private String description;

    @Schema(description = "Current processing status.", example = "IN_REVIEW",
            allowableValues = {"SUBMITTED", "IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED"})
    private ServiceRequestStatus status;

    @Schema(description = "Reference of the citizen who submitted this request.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "Display name of the citizen for context in admin/agent views.", example = "Kamal Perera")
    private String citizenName;

    @Schema(description = "Request creation timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Request last-updated timestamp.", example = "2026-06-21T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
