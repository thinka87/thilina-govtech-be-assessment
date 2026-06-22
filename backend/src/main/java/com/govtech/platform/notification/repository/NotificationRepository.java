package com.govtech.platform.notification.repository;

import com.govtech.platform.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Notification} entities.
 *
 * <p>Citizens are only permitted to read or modify their own notifications.
 * Ownership is enforced in the service layer using the citizen-scoped query methods
 * ({@link #findByIdAndCitizen_CitizenReference}) so that a single query simultaneously
 * finds the record and validates ownership, avoiding a separate ownership check.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Returns all notifications for a citizen, newest first.
     *
     * @param citizenReference the unique citizen reference
     * @return notifications ordered by {@code createdAt} descending
     */
    List<Notification> findByCitizen_CitizenReferenceOrderByCreatedAtDesc(String citizenReference);

    /**
     * Returns a paginated list of notifications for a citizen, newest first.
     * Preferred over the non-paginated variant for large notification volumes.
     *
     * @param citizenReference the unique citizen reference
     * @param pageable         pagination and sorting configuration
     * @return paginated notifications ordered by {@code createdAt} descending
     */
    Page<Notification> findByCitizen_CitizenReferenceOrderByCreatedAtDesc(
            String citizenReference, Pageable pageable);

    /**
     * Finds a notification by its ID scoped to a specific citizen.
     *
     * <p>Used in the mark-as-read flow to atomically find the notification and
     * verify the requesting citizen owns it. Returns {@link Optional#empty()} if
     * no notification with the given ID exists for that citizen — preventing
     * information disclosure about other citizens' notification IDs.</p>
     *
     * @param id               the notification primary key
     * @param citizenReference the citizen who must own the notification
     * @return the notification if found and owned, otherwise empty
     */
    Optional<Notification> findByIdAndCitizen_CitizenReference(Long id, String citizenReference);
}
