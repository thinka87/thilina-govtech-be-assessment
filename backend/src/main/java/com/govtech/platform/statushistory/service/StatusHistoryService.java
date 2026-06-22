package com.govtech.platform.statushistory.service;

import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.servicerequest.repository.ServiceRequestRepository;
import com.govtech.platform.statushistory.dto.StatusHistoryResponse;
import com.govtech.platform.statushistory.entity.StatusHistory;
import com.govtech.platform.statushistory.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Status History retrieval.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>Status history records are append-only and created exclusively by
 *       {@code ServiceRequestService} on every status transition. This service
 *       provides read-only access — no create, update, or delete operations.</li>
 *   <li>Records are returned newest-first ({@code changedAt DESC}) so agents see
 *       the most recent transition at the top of the list.</li>
 *   <li>The service validates that the parent service request exists before querying
 *       history, returning a meaningful 404 rather than an empty list for unknown
 *       request references.</li>
 *   <li>{@code oldStatus} is {@code null} for the initial SUBMITTED entry — this is
 *       by design and documented in the DTO.</li>
 * </ul>
 *
 * <h2>Auditability</h2>
 * <p>Every status transition (SUBMITTED → IN_REVIEW → APPROVED / REJECTED / CANCELLED)
 * is recorded with the actor's username, a timestamp, and optional remarks. This provides
 * a tamper-evident chronological trail that satisfies operational transparency and
 * audit requirements without modifying the main service request record.</p>
 */
@Service
@RequiredArgsConstructor
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the full status transition history for a service request, newest first.
     *
     * <p>Returns an empty list (not 404) if the service request exists but has no
     * history records yet (an edge case since SUBMITTED creates the first entry).
     * Returns 404 if the service request itself does not exist.</p>
     *
     * @param requestReference the unique service request reference (e.g. {@code REQ-8F3A91B2})
     * @return list of status history responses ordered by {@code changedAt} descending
     * @throws ResourceNotFoundException if no service request exists with the given reference
     */
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistoryByRequestReference(String requestReference) {
        // Validate parent service request existence before querying history
        if (!serviceRequestRepository.findByRequestReference(requestReference).isPresent()) {
            throw new ResourceNotFoundException("Service request not found: " + requestReference);
        }

        return statusHistoryRepository
                .findByServiceRequest_RequestReferenceOrderByChangedAtDesc(requestReference)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private StatusHistoryResponse mapToResponse(StatusHistory history) {
        return StatusHistoryResponse.builder()
                .id(history.getId())
                .requestReference(history.getServiceRequest().getRequestReference())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .changedBy(history.getChangedBy())
                .remarks(history.getRemarks())
                .changedAt(history.getChangedAt())
                .build();
    }
}
