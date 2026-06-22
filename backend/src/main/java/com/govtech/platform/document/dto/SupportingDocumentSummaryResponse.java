package com.govtech.platform.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.DocumentVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight supporting document summary used in list responses.
 *
 * <p>Returned by the GET {@code /service-requests/{ref}/documents} endpoint.
 * Excludes {@code requestReference} (implicit from the URL path) and
 * {@code updatedAt} to minimise payload size when listing many documents.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportingDocumentSummaryResponse {

    private String documentReference;
    private String documentType;
    private String documentName;
    private DocumentVerificationStatus verificationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
