package com.govtech.platform.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.govtech.platform.common.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full notification response returned by citizen-facing notification endpoints.
 *
 * <p>Includes both {@code citizenReference} and {@code requestReference} so the
 * citizen can identify which service request the notification relates to without
 * a separate lookup.</p>
 *
 * <p>The {@code id} is exposed here to allow the citizen to supply it in the
 * PATCH {@code /notifications/{notificationId}/read} endpoint.</p>
 */
@Schema(description = "In-app notification for a citizen about a service request status change.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    @Schema(description = "Database primary key — use this in PATCH /notifications/{id}/read.", example = "42")
    private Long id;

    @Schema(description = "The citizen this notification was sent to.", example = "CIT-8F3A91B2")
    private String citizenReference;

    @Schema(description = "The service request this notification relates to.", example = "REQ-2B71AC4F")
    private String requestReference;

    @Schema(description = "Human-readable notification message.",
            example = "Your service request REQ-2B71AC4F is now under review.")
    private String message;

    @Schema(description = "Read state: UNREAD until the citizen marks it as read.",
            example = "UNREAD",
            allowableValues = {"UNREAD", "READ"})
    private NotificationStatus status;

    @Schema(description = "Notification creation timestamp.", example = "2026-06-21T14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
