package com.govtech.platform.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for adding supporting document metadata to a service request.
 *
 * <p>Actual file upload is outside the scope of this assessment. This record stores
 * descriptive metadata only (type, name), allowing service agents to review and
 * verify document information via API.</p>
 *
 * <h2>documentReference</h2>
 * <p>This field is optional. If omitted, the system generates a unique reference
 * automatically using {@link com.govtech.platform.common.util.ReferenceGenerator}
 * (format: {@code DOC-XXXXXXXX}). If supplied by the caller, it is validated for
 * uniqueness; a {@code 409 Conflict} is returned if it already exists.</p>
 *
 * <p>For assessment purposes, always omit {@code documentReference} and let the
 * system generate it.</p>
 */
@Schema(description = "Request payload for adding supporting document metadata to a service request. "
                    + "Note: actual file upload is not implemented — metadata only.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupportingDocumentRequest {

    @Schema(description = "Category of the document.",
            example = "NIC_COPY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Document type is required")
    private String documentType;

    @Schema(description = "Descriptive label for the document.",
            example = "National Identity Card Front Side",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Document name is required")
    private String documentName;

    @Schema(description = "Optional caller-supplied document reference (e.g. DOC-XXXXXXXX). "
                        + "If omitted, the system generates one automatically. "
                        + "Returns 409 if the supplied reference already exists.",
            example = "DOC-9A21BE7C")
    private String documentReference;
}
