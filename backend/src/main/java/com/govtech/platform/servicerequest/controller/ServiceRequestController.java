package com.govtech.platform.servicerequest.controller;

import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.common.response.ApiResponse;
import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.common.response.ValidationErrorResponse;
import com.govtech.platform.servicerequest.dto.*;
import com.govtech.platform.servicerequest.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Service Request Management APIs.
 *
 * <p>Base path: {@code /api/v1/service-requests} (context-path {@code /api} is global)</p>
 */
@RestController
@RequestMapping("/v1/service-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Service Request Management", description = "APIs for creating, tracking, and processing citizen service requests.")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    private static final List<String> SERVICE_TYPES = List.of(
            "PASSPORT_RENEWAL",
            "BIRTH_CERTIFICATE",
            "DRIVING_LICENSE",
            "BUSINESS_REGISTRATION",
            "OTHER"
    );

    // ── GET /v1/service-requests/types ───────────────────────────────────────

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List available service types (ALL roles)",
               description = "Returns the allowed service type values for use in dropdowns. Response is wrapped in ApiResponse — the string array is under the `data` field.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Service types returned.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — authenticated user required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<String>>> getServiceTypes() {
        return ResponseEntity.ok(ApiResponse.success("Service types retrieved successfully", SERVICE_TYPES));
    }

    // ── POST /v1/service-requests ─────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN')")
    @Operation(
            summary     = "Create service request (CITIZEN, ADMIN)",
            description = "Creates a service request. "
                        + "CITIZEN: derives citizen from JWT — omit citizenReference in body. "
                        + "ADMIN: must supply citizenReference in the body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Service request created with status SUBMITTED.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceRequestResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or missing citizenReference for ADMIN caller.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — CITIZEN or ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Citizen not found (ADMIN caller, invalid citizenReference).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestRequest request) {
        ServiceRequestResponse created = serviceRequestService.createServiceRequest(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service request created successfully.", created));
    }

    // ── GET /v1/service-requests/{requestReference} ───────────────────────────

    @GetMapping("/{requestReference}")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    @Operation(
            summary     = "Get service request by reference (SERVICE_AGENT, ADMIN)",
            description = "Returns the full service request profile. Returns 404 if not found."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Service request profile returned.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceRequestResponse.class))),
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
    public ResponseEntity<ServiceRequestResponse> getServiceRequestByReference(
            @Parameter(description = "Unique request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestByReference(requestReference));
    }

    // ── GET /v1/service-requests ──────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    @Operation(
            summary     = "Search service requests with pagination (SERVICE_AGENT, ADMIN)",
            description = "Returns paginated summaries. All filters are optional and combined with AND logic. "
                        + "citizenReference filters by citizen, status filters by exact status, "
                        + "serviceType does a partial case-insensitive match."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Paginated service request list returned (may be empty).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT or ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ServiceRequestSummaryResponse>> searchServiceRequests(
            @Parameter(description = "Filter by citizen reference, e.g. CIT-8F3A91B2")
            @RequestParam(required = false) String citizenReference,
            @Parameter(description = "Filter by exact status: SUBMITTED, IN_REVIEW, APPROVED, REJECTED, CANCELLED")
            @RequestParam(required = false) ServiceRequestStatus status,
            @Parameter(description = "Partial, case-insensitive match on service type, e.g. PASSPORT")
            @RequestParam(required = false) String serviceType,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(
                serviceRequestService.searchServiceRequests(citizenReference, status, serviceType, pageable));
    }

    // ── PUT /v1/service-requests/{requestReference} ───────────────────────────

    @PutMapping("/{requestReference}")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    @Operation(
            summary     = "Update service request details (SERVICE_AGENT)",
            description = "Updates serviceType and description. Only allowed while status is SUBMITTED. "
                        + "Returns 400 if the request is in any other status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Service request updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceRequestResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or request is not in SUBMITTED status.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Service request not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ServiceRequestResponse> updateServiceRequest(
            @Parameter(description = "Unique request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference,
            @Valid @RequestBody UpdateServiceRequestRequest request) {
        return ResponseEntity.ok(serviceRequestService.updateServiceRequest(requestReference, request));
    }

    // ── PATCH /v1/service-requests/{requestReference}/status ─────────────────

    @PatchMapping("/{requestReference}/status")
    @PreAuthorize("hasRole('SERVICE_AGENT')")
    @Operation(
            summary     = "Update service request status (SERVICE_AGENT)",
            description = "Performs a status transition. Valid transitions: "
                        + "SUBMITTED→IN_REVIEW, IN_REVIEW→APPROVED|REJECTED. "
                        + "Creates a StatusHistory record and citizen Notification atomically."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Status updated. StatusHistory and Notification created.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StatusUpdateResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid status transition or terminal state.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — SERVICE_AGENT role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Service request not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<StatusUpdateResponse>> updateServiceRequestStatus(
            @Parameter(description = "Unique request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference,
            @Valid @RequestBody UpdateServiceRequestStatusRequest request) {
        StatusUpdateResponse result = serviceRequestService.updateServiceRequestStatus(requestReference, request);
        return ResponseEntity.ok(ApiResponse.success("Service request status updated successfully.", result));
    }

    // ── DELETE /v1/service-requests/{requestReference} ───────────────────────

    @DeleteMapping("/{requestReference}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Cancel service request (ADMIN) — soft cancel",
            description = "Sets status to CANCELLED. Creates history record and citizen notification. "
                        + "Returns 400 if already in a terminal state (APPROVED, REJECTED, CANCELLED)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Service request cancelled.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Bad request — request is already in a terminal state.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Service request not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> cancelServiceRequest(
            @Parameter(description = "Unique request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference) {
        serviceRequestService.cancelServiceRequest(requestReference);
        return ResponseEntity.ok(
                ApiResponse.success("Service request cancelled successfully."));
    }
}
