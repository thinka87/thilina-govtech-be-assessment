package com.govtech.platform.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the metadata of an existing supporting document.
 *
 * <p>Only {@code documentType} and {@code documentName} can be changed.
 * The {@code verificationStatus} must be updated separately via the
 * PATCH {@code /verification-status} endpoint to keep the audit intent clear.</p>
 *
 * <p>Access: <strong>SERVICE_AGENT only</strong></p>
 */
@Schema(description = "Request payload for updating document type and name. "
                    + "Use PATCH /verification-status to update verification status separately.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupportingDocumentRequest {

    @Schema(description = "Updated document type category.",
            example = "PASSPORT_COPY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Document type is required")
    private String documentType;

    @Schema(description = "Updated descriptive label for the document.",
            example = "Passport Bio Page Copy",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Document name is required")
    private String documentName;
}
