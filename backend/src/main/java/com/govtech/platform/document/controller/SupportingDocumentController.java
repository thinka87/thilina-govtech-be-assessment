package com.govtech.platform.document.controller;

import com.govtech.platform.common.response.ApiResponse;
import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.ValidationErrorResponse;
import com.govtech.platform.document.dto.*;
import com.govtech.platform.document.service.SupportingDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Supporting Document Management APIs.
 *
 * <p>Base path: {@code /api/v1}. Endpoints span two sub-paths:</p>
 * <ul>
 *   <li>{@code /v1/service-requests/{ref}/documents} — scoped to a service request</li>
 *   <li>{@code /v1/documents/{ref}}                  — scoped to a specific document</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Only document metadata is stored — no binary file upload.</p>
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Supporting Document Management",
     description = "APIs for managing supporting document metadata. "
                 + "Note: actual file upload is not implemented — metadata only (documentType, documentName, verificationStatus).")
public class SupportingDocumentController {

    private final SupportingDocumentService documentService;

    // ── POST /v1/service-requests/{requestReference}/documents ────────────────

    @PostMapping("/service-requests/{requestReference}/documents")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(
            summary     = "Add document metadata to service request (CITIZEN)",
            description = "Creates a supporting document metadata record. "
                        + "Only the citizen who owns the service request may add documents. "
                        + "Document is created with verificationStatus = PENDING."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Document metadata added with status PENDING.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SupportingDocumentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error — documentType or documentName missing.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — CITIZEN role required, or this service request does not belong to you.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Service request not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Conflict — supplied documentReference already exists.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SupportingDocumentResponse>> addDocument(
            @Parameter(description = "Service request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference,
            @Valid @RequestBody CreateSupportingDocumentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        SupportingDocumentResponse created = documentService.addDocumentToServiceRequest(
                requestReference, request, userDetails.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supporting document added successfully.", created));
    }

    // ── GET /v1/service-requests/{requestReference}/documents ─────────────────

    @GetMapping("/service-requests/{requestReference}/documents")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    @Operation(
            summary     = "List documents for a service request (SERVICE_AGENT, ADMIN)",
            description = "Returns all document metadata records attached to the given service request. "
                        + "Returns empty list if none exist. Returns 404 if service request not found."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document list returned (may be empty).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SupportingDocumentSummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT or ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Service request not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SupportingDocumentSummaryResponse>> getDocumentsByServiceRequest(
            @Parameter(description = "Service request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference) {
        return ResponseEntity.ok(documentService.getDocumentsByServiceRequest(requestReference));
    }

    // ── GET /v1/documents/{documentReference} ────────────────────────────────

    @GetMapping("/documents/{documentReference}")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    @Operation(
            summary     = "Get document by reference (SERVICE_AGENT, ADMIN)",
            description = "Returns full document metadata. Returns 404 if not found."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document metadata returned.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SupportingDocumentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT or ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupportingDocumentResponse> getDocumentByReference(
            @Parameter(description = "Document reference, e.g. DOC-9A21BE7C")
            @PathVariable String documentReference) {
        return ResponseEntity.ok(documentService.getDocumentByReference(documentReference));
    }

    // ── PUT /v1/documents/{documentReference} ────────────────────────────────

    @PutMapping("/documents/{documentReference}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    @Operation(
            summary     = "Update document metadata (SERVICE_AGENT)",
            description = "Updates documentType and documentName. "
                        + "Use PATCH /verification-status to update verification status separately."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document metadata updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SupportingDocumentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupportingDocumentResponse> updateDocument(
            @Parameter(description = "Document reference, e.g. DOC-9A21BE7C")
            @PathVariable String documentReference,
            @Valid @RequestBody UpdateSupportingDocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(documentReference, request));
    }

    // ── PATCH /v1/documents/{documentReference}/verification-status ───────────

    @PatchMapping("/documents/{documentReference}/verification-status")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    @Operation(
            summary     = "Update document verification status (SERVICE_AGENT)",
            description = "Sets verificationStatus to PENDING, VERIFIED, or REJECTED. "
                        + "Include remarks especially when rejecting a document."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Verification status updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SupportingDocumentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error — verificationStatus is null.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SupportingDocumentResponse>> updateVerificationStatus(
            @Parameter(description = "Document reference, e.g. DOC-9A21BE7C")
            @PathVariable String documentReference,
            @Valid @RequestBody UpdateDocumentVerificationStatusRequest request) {
        SupportingDocumentResponse updated = documentService.updateVerificationStatus(documentReference, request);
        return ResponseEntity.ok(
                ApiResponse.success("Document verification status updated successfully.", updated));
    }

    // ── DELETE /v1/documents/{documentReference} ──────────────────────────────

    @DeleteMapping("/documents/{documentReference}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Delete document metadata (ADMIN) — hard delete",
            description = "Permanently removes the document metadata record. "
                        + "No binary file exists to delete. Parent service request is unaffected."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Document deleted.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Document not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "Document reference, e.g. DOC-9A21BE7C")
            @PathVariable String documentReference) {
        documentService.deleteDocument(documentReference);
        return ResponseEntity.ok(
                ApiResponse.success("Supporting document deleted successfully."));
    }
}
