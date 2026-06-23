package com.govtech.platform.citizen.controller;

import com.govtech.platform.citizen.dto.*;
import com.govtech.platform.citizen.service.CitizenService;
import com.govtech.platform.common.enums.CitizenStatus;
import com.govtech.platform.common.response.ApiResponse;
import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.common.response.ValidationErrorResponse;
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

/**
 * REST controller for Citizen Management APIs.
 *
 * <p>Base path: {@code /api/v1/citizens} (context-path {@code /api} is global)</p>
 *
 * <h2>Access control</h2>
 * <table border="1">
 *   <tr><th>Endpoint</th><th>Role</th></tr>
 *   <tr><td>POST   /</td><td>ADMIN</td></tr>
 *   <tr><td>GET    /{ref}</td><td>ADMIN, SERVICE_AGENT</td></tr>
 *   <tr><td>GET    /</td><td>ADMIN</td></tr>
 *   <tr><td>PUT    /{ref}</td><td>ADMIN</td></tr>
 *   <tr><td>DELETE /{ref}</td><td>ADMIN (soft deactivate)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/v1/citizens")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Citizen Management", description = "Admin APIs for creating, searching, updating, and deactivating citizen profiles.")
public class CitizenController {

    private final CitizenService citizenService;

    // ── POST /v1/citizens ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Create citizen profile (ADMIN)",
            description = "Creates a citizen profile and a linked login account. "
                        + "The citizen's email is used as their login username. "
                        + "Returns 409 if NIC or email is already registered."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Citizen created successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CitizenCreatedResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error — required fields missing or invalid.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Conflict — NIC or email already registered.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CitizenCreatedResponse>> createCitizen(
            @Valid @RequestBody CreateCitizenRequest request) {
        CitizenCreatedResponse created = citizenService.createCitizen(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Citizen created successfully.", created));
    }

    // ── GET /v1/citizens/{citizenReference} ───────────────────────────────────

    @GetMapping("/{citizenReference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_AGENT')")
    @Operation(
            summary     = "View citizen information (ADMIN, SERVICE_AGENT)",
            description = "Allows an administrator or service agent to view citizen details by citizen reference "
                        + "before processing a service request. Returns 404 if the citizen does not exist. "
                        + "CITIZEN role is not permitted to call this endpoint."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Citizen profile returned.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CitizenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN or SERVICE_AGENT role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Citizen not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitizenResponse> getCitizenByReference(
            @Parameter(description = "Unique citizen reference, e.g. CIT-8F3A91B2")
            @PathVariable String citizenReference) {
        return ResponseEntity.ok(citizenService.getCitizenByReference(citizenReference));
    }

    // ── GET /v1/citizens ──────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "List / search citizens with pagination (ADMIN)",
            description = "Returns paginated citizen summaries. "
                        + "Optional 'search' param filters by name, NIC, email, or mobile number. "
                        + "Default sort: createdAt DESC."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Paginated citizen list returned (may be empty).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<CitizenSummaryResponse>> listCitizens(
            @Parameter(description = "Optional keyword to search by name, NIC, email, or mobile number")
            @RequestParam(required = false) String search,
            @Parameter(description = "Optional status filter: ACTIVE or INACTIVE")
            @RequestParam(required = false) CitizenStatus status,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(citizenService.listCitizens(search, status, pageable));
    }

    // ── PUT /v1/citizens/{citizenReference} ───────────────────────────────────

    @PutMapping("/{citizenReference}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Update citizen profile (ADMIN)",
            description = "Updates citizen information. NIC cannot be changed. "
                        + "Changing the email also updates the linked User's username and invalidates existing JWTs."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Citizen profile updated.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CitizenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — ADMIN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Citizen not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Conflict — new email already in use by another account.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitizenResponse> updateCitizen(
            @Parameter(description = "Unique citizen reference, e.g. CIT-8F3A91B2")
            @PathVariable String citizenReference,
            @Valid @RequestBody UpdateCitizenRequest request) {
        return ResponseEntity.ok(citizenService.updateCitizen(citizenReference, request));
    }

    // ── DELETE /v1/citizens/{citizenReference} ────────────────────────────────

    @DeleteMapping("/{citizenReference}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Deactivate citizen (ADMIN) — soft delete",
            description = "Sets citizen status to INACTIVE and disables login. "
                        + "Data is preserved. Returns 400 if already inactive."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Citizen deactivated successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Bad request — citizen is already inactive.",
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
                    description = "Citizen not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deactivateCitizen(
            @Parameter(description = "Unique citizen reference, e.g. CIT-8F3A91B2")
            @PathVariable String citizenReference) {
        citizenService.deactivateCitizen(citizenReference);
        return ResponseEntity.ok(
                ApiResponse.success("Citizen deactivated successfully. Login access has been revoked."));
    }
}
