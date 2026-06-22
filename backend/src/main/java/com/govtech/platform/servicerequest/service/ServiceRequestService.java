package com.govtech.platform.servicerequest.service;

import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.NotificationStatus;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.InvalidStatusException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.common.exception.UnauthorizedActionException;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.common.util.ReferenceGenerator;
import com.govtech.platform.notification.entity.Notification;
import com.govtech.platform.document.dto.SupportingDocumentSummaryResponse;
import com.govtech.platform.document.entity.SupportingDocument;
import com.govtech.platform.document.repository.SupportingDocumentRepository;
import com.govtech.platform.notification.repository.NotificationRepository;
import com.govtech.platform.servicerequest.dto.*;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import com.govtech.platform.servicerequest.repository.ServiceRequestRepository;
import com.govtech.platform.servicerequest.repository.ServiceRequestSpecification;
import com.govtech.platform.statushistory.entity.StatusHistory;
import com.govtech.platform.statushistory.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for Service Request Management.
 *
 * <h2>Role-based behaviour summary</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>CITIZEN</th><th>SERVICE_AGENT</th><th>ADMIN</th></tr>
 *   <tr><td>createServiceRequest</td><td>Creates for self (derived from JWT)</td><td>-</td><td>Creates for any citizen (citizenReference required)</td></tr>
 *   <tr><td>getServiceRequestByReference</td><td>-</td><td>Any request</td><td>Any request</td></tr>
 *   <tr><td>getMyCitizenServiceRequests</td><td>Own requests only</td><td>-</td><td>-</td></tr>
 *   <tr><td>searchServiceRequests</td><td>-</td><td>All / filtered</td><td>All / filtered</td></tr>
 *   <tr><td>updateServiceRequest</td><td>-</td><td>SUBMITTED only</td><td>-</td></tr>
 *   <tr><td>updateServiceRequestStatus</td><td>-</td><td>Valid transitions</td><td>-</td></tr>
 *   <tr><td>cancelServiceRequest</td><td>-</td><td>-</td><td>Non-terminal requests</td></tr>
 * </table>
 *
 * <h2>Status transition rules</h2>
 * <ul>
 *   <li>SUBMITTED  → IN_REVIEW  (agent picks up)</li>
 *   <li>SUBMITTED  → CANCELLED  (admin cancels)</li>
 *   <li>IN_REVIEW  → APPROVED   (agent approves)</li>
 *   <li>IN_REVIEW  → REJECTED   (agent rejects)</li>
 *   <li>IN_REVIEW  → CANCELLED  (admin cancels during review)</li>
 *   <li>APPROVED, REJECTED, CANCELLED are terminal — no further transitions.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final CitizenRepository citizenRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final SupportingDocumentRepository supportingDocumentRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new service request.
     *
     * <p>For CITIZEN callers the citizen profile is resolved from the authenticated
     * JWT principal. For ADMIN callers, {@code request.getCitizenReference()} must
     * be provided and must match an existing active citizen.</p>
     *
     * <p>An initial {@link StatusHistory} entry is created atomically with the
     * request (oldStatus=null, newStatus=SUBMITTED).</p>
     *
     * @param request the creation payload
     * @return full service request response
     */
    @Transactional
    public ServiceRequestResponse createServiceRequest(CreateServiceRequestRequest request) {
        String authenticatedUsername = getAuthenticatedUsername();
        Citizen citizen;

        if (hasRole("CITIZEN")) {
            // Citizen creates for themselves — derive their profile from auth
            citizen = citizenRepository.findByUser_Username(authenticatedUsername)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No citizen profile linked to account: " + authenticatedUsername));
        } else {
            // ADMIN creates on behalf of a citizen — citizenReference required
            if (request.getCitizenReference() == null || request.getCitizenReference().isBlank()) {
                throw new BusinessException("citizenReference is required when creating a request as ADMIN");
            }
            citizen = citizenRepository.findByCitizenReference(request.getCitizenReference())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Citizen not found: " + request.getCitizenReference()));
        }

        // Generate unique reference
        String requestReference = generateUniqueReference();

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .requestReference(requestReference)
                .citizen(citizen)
                .serviceType(request.getServiceType().trim())
                .description(request.getDescription().trim())
                .status(ServiceRequestStatus.SUBMITTED)
                .build();

        serviceRequest = serviceRequestRepository.save(serviceRequest);

        // Append initial status history (no previous state)
        appendStatusHistory(serviceRequest, null, ServiceRequestStatus.SUBMITTED,
                authenticatedUsername, "Service request submitted.");

        return mapToResponse(serviceRequest);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the full service request profile for a given reference.
     * No ownership check — access is controlled at the controller level via {@code @PreAuthorize}.
     *
     * @param requestReference unique request reference (e.g. {@code REQ-8F3A91B2})
     * @return full service request response
     */
    @Transactional(readOnly = true)
    public ServiceRequestResponse getServiceRequestByReference(String requestReference) {
        ServiceRequest sr = findOrThrow(requestReference);
        return mapToResponse(sr);
    }

    /**
     * Returns a paginated list of service requests belonging to a specific citizen.
     * Enforces ownership: the authenticated CITIZEN user must own the citizen profile.
     *
     * @param citizenReference  the citizen whose requests are requested
     * @param pageable          pagination configuration
     * @return paged list of summary responses
     */
    @Transactional(readOnly = true)
    public PageResponse<ServiceRequestSummaryResponse> getMyCitizenServiceRequests(
            String citizenReference, Pageable pageable) {

        String authenticatedUsername = getAuthenticatedUsername();

        // Verify the authenticated citizen owns this citizenReference
        Citizen citizen = citizenRepository.findByCitizenReference(citizenReference)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + citizenReference));

        if (!citizen.getUser().getUsername().equals(authenticatedUsername)) {
            throw new UnauthorizedActionException(
                    "You are not authorised to view service requests for citizen: " + citizenReference);
        }

        Page<ServiceRequest> page = serviceRequestRepository
                .findByCitizen_CitizenReference(citizenReference, pageable);
        return PageResponse.from(page.map(sr -> {
            List<SupportingDocumentSummaryResponse> docs =
                    supportingDocumentRepository
                            .findByServiceRequest_RequestReference(sr.getRequestReference())
                            .stream()
                            .map(this::mapToDocumentSummary)
                            .collect(Collectors.toList());
            return ServiceRequestSummaryResponse.builder()
                    .requestReference(sr.getRequestReference())
                    .serviceType(sr.getServiceType())
                    .status(sr.getStatus())
                    .citizenReference(sr.getCitizen().getCitizenReference())
                    .createdAt(sr.getCreatedAt())
                    .documents(docs)
                    .build();
        }));
    }

    /**
     * Returns a paginated, dynamically filtered list of service requests for agents/admins.
     * All filters are optional and are combined with AND logic.
     *
     * @param citizenReference optional citizen reference to scope results
     * @param status           optional status filter
     * @param serviceType      optional partial service type match (case-insensitive)
     * @param pageable         pagination configuration
     * @return paged list of summary responses
     */
    @Transactional(readOnly = true)
    public PageResponse<ServiceRequestSummaryResponse> searchServiceRequests(
            String citizenReference,
            ServiceRequestStatus status,
            String serviceType,
            Pageable pageable) {

        Specification<ServiceRequest> spec = Specification.where(null);

        if (citizenReference != null && !citizenReference.isBlank()) {
            spec = spec.and(ServiceRequestSpecification.byCitizenReference(citizenReference));
        }
        if (status != null) {
            spec = spec.and(ServiceRequestSpecification.byStatus(status));
        }
        if (serviceType != null && !serviceType.isBlank()) {
            spec = spec.and(ServiceRequestSpecification.byServiceTypeContaining(serviceType));
        }

        Page<ServiceRequest> page = serviceRequestRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::mapToSummaryResponse));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates the service type and description of a service request.
     * Only permitted while the request is in {@code SUBMITTED} status.
     *
     * @param requestReference unique request reference
     * @param request          update payload
     * @return updated full service request response
     */
    @Transactional
    public ServiceRequestResponse updateServiceRequest(
            String requestReference, UpdateServiceRequestRequest request) {

        ServiceRequest sr = findOrThrow(requestReference);

        if (sr.getStatus() != ServiceRequestStatus.SUBMITTED) {
            throw new InvalidStatusException(
                    "Service request can only be updated while in SUBMITTED status. "
                            + "Current status: " + sr.getStatus());
        }

        sr.setServiceType(request.getServiceType().trim());
        sr.setDescription(request.getDescription().trim());
        sr = serviceRequestRepository.save(sr);

        return mapToResponse(sr);
    }

    /**
     * Advances a service request to a new status.
     *
     * <p>Validates the status transition, persists the change, appends a
     * {@link StatusHistory} record, and creates a {@link Notification} for the citizen.</p>
     *
     * @param requestReference unique request reference
     * @param request          status update payload (target status + optional remarks)
     * @return status update summary response
     */
    @Transactional
    public StatusUpdateResponse updateServiceRequestStatus(
            String requestReference, UpdateServiceRequestStatusRequest request) {

        ServiceRequest sr = findOrThrow(requestReference);
        ServiceRequestStatus previousStatus = sr.getStatus();
        ServiceRequestStatus targetStatus = request.getStatus();

        validateStatusTransition(previousStatus, targetStatus);

        sr.setStatus(targetStatus);
        serviceRequestRepository.save(sr);

        String authenticatedUsername = getAuthenticatedUsername();
        appendStatusHistory(sr, previousStatus, targetStatus,
                authenticatedUsername, request.getRemarks());

        createNotification(sr, targetStatus, request.getRemarks());

        return StatusUpdateResponse.builder()
                .requestReference(sr.getRequestReference())
                .previousStatus(previousStatus)
                .newStatus(targetStatus)
                .remarks(request.getRemarks())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }

    // ── Cancel (ADMIN) ────────────────────────────────────────────────────────

    /**
     * Cancels a service request (ADMIN only, soft cancel).
     *
     * <p>Permitted from {@code SUBMITTED} or {@code IN_REVIEW} states only.
     * Terminal states ({@code APPROVED}, {@code REJECTED}, {@code CANCELLED})
     * cannot be cancelled.</p>
     *
     * @param requestReference unique request reference
     */
    @Transactional
    public void cancelServiceRequest(String requestReference) {
        ServiceRequest sr = findOrThrow(requestReference);
        ServiceRequestStatus current = sr.getStatus();

        if (current == ServiceRequestStatus.APPROVED
                || current == ServiceRequestStatus.REJECTED
                || current == ServiceRequestStatus.CANCELLED) {
            throw new InvalidStatusException(
                    "Cannot cancel a request that is already " + current
                            + ". Only SUBMITTED or IN_REVIEW requests can be cancelled.");
        }

        sr.setStatus(ServiceRequestStatus.CANCELLED);
        serviceRequestRepository.save(sr);

        String authenticatedUsername = getAuthenticatedUsername();
        appendStatusHistory(sr, current, ServiceRequestStatus.CANCELLED,
                authenticatedUsername, "Cancelled by administrator.");

        createNotification(sr, ServiceRequestStatus.CANCELLED, "Your service request has been cancelled by an administrator.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ServiceRequest findOrThrow(String requestReference) {
        return serviceRequestRepository.findByRequestReference(requestReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found: " + requestReference));
    }

    private void validateStatusTransition(ServiceRequestStatus current, ServiceRequestStatus target) {
        boolean valid = switch (current) {
            case SUBMITTED -> target == ServiceRequestStatus.IN_REVIEW
                    || target == ServiceRequestStatus.CANCELLED;
            case IN_REVIEW -> target == ServiceRequestStatus.APPROVED
                    || target == ServiceRequestStatus.REJECTED
                    || target == ServiceRequestStatus.CANCELLED;
            case APPROVED, REJECTED, CANCELLED -> false; // terminal states
        };

        if (!valid) {
            throw new InvalidStatusException(
                    "Invalid status transition: " + current + " → " + target
                            + ". Allowed from " + current + ": " + allowedTransitions(current));
        }
    }

    private String allowedTransitions(ServiceRequestStatus current) {
        return switch (current) {
            case SUBMITTED -> "[IN_REVIEW, CANCELLED]";
            case IN_REVIEW -> "[APPROVED, REJECTED, CANCELLED]";
            case APPROVED, REJECTED, CANCELLED -> "none (terminal state)";
        };
    }

    private void appendStatusHistory(ServiceRequest sr, ServiceRequestStatus oldStatus,
                                     ServiceRequestStatus newStatus, String changedBy, String remarks) {
        StatusHistory history = StatusHistory.builder()
                .serviceRequest(sr)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .remarks(remarks)
                .build();
        statusHistoryRepository.save(history);
    }

    private void createNotification(ServiceRequest sr, ServiceRequestStatus newStatus, String extraMessage) {
        String message = buildNotificationMessage(sr.getRequestReference(), newStatus, extraMessage);
        Notification notification = Notification.builder()
                .citizen(sr.getCitizen())
                .serviceRequest(sr)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .build();
        notificationRepository.save(notification);
    }

    private String buildNotificationMessage(String ref, ServiceRequestStatus status, String extra) {
        String base = switch (status) {
            case IN_REVIEW  -> "Your service request " + ref + " is now under review.";
            case APPROVED   -> "Your service request " + ref + " has been approved.";
            case REJECTED   -> "Your service request " + ref + " has been rejected.";
            case CANCELLED  -> extra != null ? extra : "Your service request " + ref + " has been cancelled.";
            case SUBMITTED  -> "Your service request " + ref + " has been submitted successfully.";
        };
        return base;
    }

    private String generateUniqueReference() {
        String ref;
        int attempts = 0;
        do {
            ref = ReferenceGenerator.generateServiceRequestReference();
            if (++attempts > 10) {
                throw new BusinessException("Failed to generate a unique request reference. Please retry.");
            }
        } while (serviceRequestRepository.existsByRequestReference(ref));
        return ref;
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private ServiceRequestResponse mapToResponse(ServiceRequest sr) {
        return ServiceRequestResponse.builder()
                .requestReference(sr.getRequestReference())
                .serviceType(sr.getServiceType())
                .description(sr.getDescription())
                .status(sr.getStatus())
                .citizenReference(sr.getCitizen().getCitizenReference())
                .citizenName(sr.getCitizen().getName())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }

    private ServiceRequestSummaryResponse mapToSummaryResponse(ServiceRequest sr) {
        return ServiceRequestSummaryResponse.builder()
                .requestReference(sr.getRequestReference())
                .serviceType(sr.getServiceType())
                .status(sr.getStatus())
                .citizenReference(sr.getCitizen().getCitizenReference())
                .createdAt(sr.getCreatedAt())
                .build();
    }

    private SupportingDocumentSummaryResponse mapToDocumentSummary(SupportingDocument doc) {
        return SupportingDocumentSummaryResponse.builder()
                .documentReference(doc.getDocumentReference())
                .documentType(doc.getDocumentType())
                .documentName(doc.getDocumentName())
                .verificationStatus(doc.getVerificationStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
