package com.govtech.platform.citizen.controller;

import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.servicerequest.dto.ServiceRequestSummaryResponse;
import com.govtech.platform.servicerequest.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for citizen self-service endpoints.
 *
 * <p>Base path: {@code /api/v1/citizens} (context-path {@code /api} is global).
 * Provides citizens read access to their own service requests only.</p>
 */
@RestController
@RequestMapping("/v1/citizens")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Citizen Self-Service", description = "Citizen-only endpoints for viewing their own service requests.")
public class CitizenSelfServiceController {

    private final ServiceRequestService serviceRequestService;

    // ── GET /v1/citizens/{citizenReference}/service-requests ──────────────────

    @GetMapping("/{citizenReference}/service-requests")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(
            summary     = "List my service requests (CITIZEN)",
            description = "Returns the authenticated citizen's own paginated service request list, newest-first. "
                        + "Returns 403 if the citizenReference does not match the authenticated user. "
                        + "Returns empty list if no requests exist."
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
                    description = "Forbidden — CITIZEN role required, or citizenReference does not match authenticated user.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Citizen not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ServiceRequestSummaryResponse>> getMyServiceRequests(
            @Parameter(description = "Citizen reference matching the authenticated user, e.g. CIT-8F3A91B2")
            @PathVariable String citizenReference,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(
                serviceRequestService.getMyCitizenServiceRequests(citizenReference, pageable));
    }
}
