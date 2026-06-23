package com.govtech.platform.notification.controller;

import com.govtech.platform.common.response.ApiResponse;
import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.notification.dto.MarkNotificationReadResponse;
import com.govtech.platform.notification.dto.NotificationResponse;
import com.govtech.platform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Citizen Notification APIs.
 *
 * <p>Base path: {@code /api/v1}. Endpoints:</p>
 * <ul>
 *   <li>{@code GET  /v1/citizens/{citizenReference}/notifications} — list</li>
 *   <li>{@code PATCH /v1/notifications/{notificationId}/read}      — mark read</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications",
     description = "Citizen-only APIs for listing in-app notifications and marking them as read. "
                 + "Notifications are created automatically when service request status changes.")
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET /v1/citizens/{citizenReference}/notifications ────────────────────

    @GetMapping("/citizens/{citizenReference}/notifications")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(
            summary     = "Get my notifications (CITIZEN)",
            description = "Returns the authenticated citizen's own notifications, newest-first. "
                        + "Returns 403 if the citizenReference does not match the authenticated user. "
                        + "Returns empty page if no notifications exist."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Paginated notification list returned (may be empty).",
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
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @Parameter(description = "Citizen reference matching the authenticated user, e.g. CIT-8F3A91B2")
            @PathVariable String citizenReference,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByCitizen(
                        citizenReference, userDetails.getUsername(), pageable));
    }

    // ── PATCH /v1/notifications/{notificationId}/read ────────────────────────

    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(
            summary     = "Mark notification as read (CITIZEN)",
            description = "Sets the notification status to READ. Idempotent — succeeds if already read. "
                        + "Returns 404 if the notification does not exist or belongs to another citizen "
                        + "(prevents ID enumeration)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Notification marked as read.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MarkNotificationReadResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing or invalid JWT token.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden — CITIZEN role required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Notification not found or does not belong to the authenticated citizen.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<MarkNotificationReadResponse>> markAsRead(
            @Parameter(description = "The numeric ID of the notification (from the id field in NotificationResponse)")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        MarkNotificationReadResponse result =
                notificationService.markNotificationAsRead(notificationId, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }
}
