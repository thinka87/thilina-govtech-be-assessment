package com.govtech.platform.notification.service;

import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.NotificationStatus;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.common.exception.UnauthorizedActionException;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.notification.dto.MarkNotificationReadResponse;
import com.govtech.platform.notification.dto.NotificationResponse;
import com.govtech.platform.notification.entity.Notification;
import com.govtech.platform.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for Notification Management.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>Notifications are created automatically by {@code ServiceRequestService} when
 *       a service request status changes. This service only <em>reads</em> and
 *       <em>marks-as-read</em> — it never creates notifications directly.</li>
 *   <li>Citizens may only access their own notifications. Ownership is validated in
 *       every read path by comparing the authenticated username with the username of
 *       the User account linked to the citizen profile.</li>
 *   <li>The mark-as-read query ({@code findByIdAndCitizen_CitizenReference}) performs
 *       a single JOIN that both retrieves the notification and verifies ownership,
 *       preventing information disclosure about other citizens' notification IDs.</li>
 *   <li>Notifications are only readable, never deletable, to preserve the communication
 *       audit trail between the government platform and citizens.</li>
 * </ul>
 *
 * <h2>Notification lifecycle</h2>
 * <ol>
 *   <li>Service agent updates a service request status.</li>
 *   <li>{@code ServiceRequestService.updateServiceRequestStatus} atomically creates
 *       a {@code Notification} record with {@code status = UNREAD}.</li>
 *   <li>Citizen fetches their notifications via GET {@code /citizens/{ref}/notifications}.</li>
 *   <li>Citizen marks a notification as read via PATCH {@code /notifications/{id}/read}.</li>
 * </ol>
 *
 * <h2>Production note</h2>
 * <p>This implementation stores notifications in the database only. In production,
 * notification records could trigger delivery via email, SMS, or push notifications
 * through an external provider (SendGrid, Twilio, Firebase). The DB record serves as
 * the persistent in-app notification and delivery confirmation log.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CitizenRepository citizenRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of notifications for a citizen, newest first.
     *
     * <h3>Ownership validation</h3>
     * <p>The citizen profile is looked up by {@code citizenReference}. The authenticated
     * user's username is then compared with the username of the User account linked to
     * that citizen. A mismatch throws {@link UnauthorizedActionException} ({@code 403}).</p>
     *
     * @param citizenReference the citizen whose notifications are requested
     * @param currentUsername  the authenticated user's username (from JWT principal)
     * @param pageable         pagination configuration
     * @return paginated notification responses, empty if none exist
     * @throws ResourceNotFoundException   if no citizen exists with the given reference
     * @throws UnauthorizedActionException if the authenticated user does not own the citizen profile
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsByCitizen(
            String citizenReference, String currentUsername, Pageable pageable) {

        Citizen citizen = findCitizenByReferenceOrThrow(citizenReference);
        verifyOwnership(citizen, currentUsername);

        Page<Notification> page = notificationRepository
                .findByCitizen_CitizenReferenceOrderByCreatedAtDesc(citizenReference, pageable);

        return PageResponse.from(page.map(this::mapToResponse));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Marks a notification as read for the authenticated citizen.
     *
     * <h3>Ownership validation</h3>
     * <p>Step 1: the citizen profile is resolved from {@code currentUsername}.
     * Step 2: the notification is fetched using a citizen-scoped query
     * ({@code findByIdAndCitizen_CitizenReference}) that simultaneously looks up the
     * record and verifies it belongs to the found citizen. If the notification ID
     * does not belong to that citizen, a {@link ResourceNotFoundException} is thrown
     * rather than an {@link UnauthorizedActionException}, preventing enumeration of
     * other citizens' notification IDs.</p>
     *
     * <p>If the notification is already {@code READ}, the operation is idempotent —
     * it succeeds without error.</p>
     *
     * @param notificationId  the primary key of the notification to mark as read
     * @param currentUsername the authenticated citizen's username
     * @return lightweight confirmation response with updated status
     * @throws ResourceNotFoundException if the notification does not exist or does not belong to the citizen
     */
    @Transactional
    public MarkNotificationReadResponse markNotificationAsRead(Long notificationId, String currentUsername) {

        // Step 1: resolve the authenticated citizen's profile
        Citizen citizen = citizenRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No citizen profile linked to account: " + currentUsername));

        // Step 2: citizen-scoped lookup — finds notification AND verifies ownership in one query
        Notification notification = notificationRepository
                .findByIdAndCitizen_CitizenReference(notificationId, citizen.getCitizenReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        // Idempotent — no-op if already READ
        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }

        return MarkNotificationReadResponse.builder()
                .id(notification.getId())
                .status(notification.getStatus())
                .message("Notification marked as read.")
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Citizen findCitizenByReferenceOrThrow(String citizenReference) {
        return citizenRepository.findByCitizenReference(citizenReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen not found: " + citizenReference));
    }

    /**
     * Verifies that {@code currentUsername} is the authenticated owner of the citizen profile.
     * The check traverses: {@code citizen → user → username}.
     */
    private void verifyOwnership(Citizen citizen, String currentUsername) {
        String ownerUsername = citizen.getUser().getUsername();
        if (!ownerUsername.equals(currentUsername)) {
            throw new UnauthorizedActionException(
                    "You are not authorised to view notifications for this citizen.");
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .citizenReference(notification.getCitizen().getCitizenReference())
                .requestReference(notification.getServiceRequest().getRequestReference())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
