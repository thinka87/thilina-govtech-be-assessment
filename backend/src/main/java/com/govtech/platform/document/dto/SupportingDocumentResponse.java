package com.govtech.platform.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.DocumentVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full supporting document response returned for single-document endpoints.
 *
 * <p>Includes {@code requestReference} so consumers can navigate to the parent
 * service request without a separate lookup. Excludes internal database {@code id}.</p>
 */
@Schema(description = "Full supporting document metadata response.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportingDocumentResponse {

    @Schema(description = "Unique system-generated document reference.", example = "DOC-9A21BE7C")
    private String documentReference;

    @Schema(description = "Reference of the parent service request.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "Category of the document.", example = "NIC_COPY")
    private String documentType;

    @Schema(description = "Descriptive label for the document.", example = "National Identity Card Front Side")
    private String documentName;

    @Schema(description = "Current verification status.", example = "VERIFIED",
            allowableValues = {"PENDING", "VERIFIED", "REJECTED"})
    private DocumentVerificationStatus verificationStatus;

    @Schema(description = "Document record creation timestamp.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Document record last-updated timestamp.", example = "2026-06-21T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
