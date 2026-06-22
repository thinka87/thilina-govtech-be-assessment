package com.govtech.platform.statushistory.repository;

import com.govtech.platform.statushistory.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link StatusHistory} entities.
 *
 * <p>Records are append-only; no delete or update operations are intended
 * to preserve audit integrity.</p>
 */
@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    /**
     * Returns the full status transition history for a service request,
     * most recent change first.
     *
     * @param requestReference the unique service request reference
     * @return status history entries ordered by {@code changedAt} descending
     */
    List<StatusHistory> findByServiceRequest_RequestReferenceOrderByChangedAtDesc(String requestReference);
}
