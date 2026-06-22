package com.govtech.platform.statushistory.controller;

import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.statushistory.dto.StatusHistoryResponse;
import com.govtech.platform.statushistory.service.StatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Service Request Status History APIs.
 *
 * <p>Base path: {@code /api/v1/service-requests}. Status history records are
 * immutable audit entries created on every status transition. Read-only access.</p>
 */
@RestController
@RequestMapping("/v1/service-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Status History",
     description = "Read-only audit trail of service request status transitions. "
                 + "Each transition (SUBMITTED→IN_REVIEW→APPROVED/REJECTED) is recorded with actor, timestamp, and remarks.")
public class StatusHistoryController {

    private final StatusHistoryService statusHistoryService;

    // ── GET /v1/service-requests/{requestReference}/status-history ────────────

    @GetMapping("/{requestReference}/status-history")
    @PreAuthorize("hasAnyRole('SERVICE_AGENT', 'ADMIN')")
    @Operation(
            summary     = "Get status history for a service request (SERVICE_AGENT, ADMIN)",
            description = "Returns the immutable audit trail of all status transitions, newest-first. "
                        + "oldStatus is null for the initial SUBMITTED entry (no prior state). "
                        + "Returns empty list if no transitions recorded. Returns 404 if service request not found."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Status history list returned (may be empty).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StatusHistoryResponse.class))),
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
    public ResponseEntity<List<StatusHistoryResponse>> getStatusHistory(
            @Parameter(description = "Service request reference, e.g. REQ-2B71AC4F")
            @PathVariable String requestReference) {
        return ResponseEntity.ok(
                statusHistoryService.getStatusHistoryByRequestReference(requestReference));
    }
}
