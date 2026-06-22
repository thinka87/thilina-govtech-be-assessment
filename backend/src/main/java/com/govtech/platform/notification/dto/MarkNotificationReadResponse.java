package com.govtech.platform.notification.dto;

import com.govtech.platform.common.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lightweight response returned after successfully marking a notification as read.
 *
 * <p>Returns only the fields needed to confirm the state change. Use
 * {@link NotificationResponse} when the full notification detail is required.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkNotificationReadResponse {

    /** Database primary key of the updated notification. */
    private Long id;

    /** The new status after the update — always {@code READ} on success. */
    private NotificationStatus status;

    /** Confirmation message. */
    private String message;
}
