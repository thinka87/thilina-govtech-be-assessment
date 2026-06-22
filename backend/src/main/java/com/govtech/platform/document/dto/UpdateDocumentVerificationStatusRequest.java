package com.govtech.platform.document.dto;

import com.govtech.platform.common.enums.DocumentVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the verification status of a supporting document.
 *
 * <p>Service agents use this endpoint after reviewing the physical or digital
 * document submitted by the citizen. The status can be set to:</p>
 * <ul>
 *   <li>{@code PENDING}  — reset to awaiting review (rarely used)</li>
 *   <li>{@code VERIFIED} — document is valid and accepted</li>
 *   <li>{@code REJECTED} — document is invalid, expired, or unacceptable</li>
 * </ul>
 *
 * <p>{@code remarks} is optional but recommended when rejecting a document
 * so the citizen understands the reason.</p>
 *
 * <p>Access: <strong>SERVICE_AGENT only</strong></p>
 */
@Schema(description = "Request payload for updating a supporting document's verification status.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentVerificationStatusRequest {

    @Schema(description = "The new verification status.",
            example = "VERIFIED",
            allowableValues = {"PENDING", "VERIFIED", "REJECTED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Verification status is required")
    private DocumentVerificationStatus verificationStatus;

    @Schema(description = "Optional remarks explaining the decision. Recommended when status is REJECTED.",
            example = "Document verified successfully. NIC copy is clear and valid.")
    private String remarks;
}
