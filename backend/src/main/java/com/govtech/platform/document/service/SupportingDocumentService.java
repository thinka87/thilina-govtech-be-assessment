package com.govtech.platform.document.service;

import com.govtech.platform.common.enums.DocumentVerificationStatus;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.DuplicateResourceException;
import com.govtech.platform.common.exception.InvalidStatusException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.common.exception.UnauthorizedActionException;
import com.govtech.platform.common.util.ReferenceGenerator;
import com.govtech.platform.document.dto.*;
import com.govtech.platform.document.entity.SupportingDocument;
import com.govtech.platform.document.repository.SupportingDocumentRepository;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import com.govtech.platform.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Supporting Document Management.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>Only document <strong>metadata</strong> is stored — no binary file data.
 *       Actual file upload (S3, local filesystem) is outside the scope of this assessment.</li>
 *   <li>Citizens may only add documents to service requests they own, enforced by
 *       comparing the authenticated username with the citizen linked to the request.</li>
 *   <li>{@code documentReference} is always system-generated unless the caller supplies
 *       one explicitly, in which case uniqueness is validated before use.</li>
 *   <li>Documents are physically deleted (not soft-deleted) because they represent
 *       metadata only; no binary asset needs preservation.</li>
 * </ul>
 *
 * <h2>Role-based access summary</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>CITIZEN</th><th>SERVICE_AGENT</th><th>ADMIN</th></tr>
 *   <tr><td>addDocumentToServiceRequest</td><td>Own requests only</td><td>-</td><td>-</td></tr>
 *   <tr><td>getDocumentByReference</td><td>-</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>getDocumentsByServiceRequest</td><td>-</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>updateDocument</td><td>-</td><td>✓</td><td>-</td></tr>
 *   <tr><td>updateVerificationStatus</td><td>-</td><td>✓</td><td>-</td></tr>
 *   <tr><td>deleteDocument</td><td>-</td><td>-</td><td>✓</td></tr>
 * </table>
 */
@Service
@RequiredArgsConstructor
public class SupportingDocumentService {

    private final SupportingDocumentRepository documentRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Adds supporting document metadata to an existing service request.
     *
     * <h3>Ownership rule</h3>
     * <p>When the authenticated caller has the {@code CITIZEN} role, the service request
     * must belong to that citizen. The ownership check compares the authenticated username
     * with the username of the User account linked to the request's citizen profile:
     * {@code serviceRequest → citizen → user → username}.</p>
     *
     * <h3>documentReference</h3>
     * <p>If {@code request.getDocumentReference()} is blank, a unique reference is
     * generated via {@link ReferenceGenerator#generateDocumentReference()}.
     * If a non-blank value is supplied, it is validated for global uniqueness;
     * a {@code 409 Conflict} is thrown on duplicate.</p>
     *
     * @param requestReference the service request to attach the document to
     * @param request          the document metadata payload
     * @param currentUsername  the authenticated user's username (from JWT principal)
     * @return the persisted document's full response
     * @throws ResourceNotFoundException    if the service request does not exist
     * @throws UnauthorizedActionException  if a citizen attempts to add to another citizen's request
     * @throws DuplicateResourceException   if the supplied documentReference already exists
     */
    @Transactional
    public SupportingDocumentResponse addDocumentToServiceRequest(
            String requestReference,
            CreateSupportingDocumentRequest request,
            String currentUsername) {

        ServiceRequest serviceRequest = findServiceRequestOrThrow(requestReference);

        if (serviceRequest.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new InvalidStatusException(
                    "Cannot add documents to a cancelled service request.");
        }

        // Ownership check: CITIZEN callers may only add to their own requests
        if (hasRole("CITIZEN")) {
            String ownerUsername = serviceRequest.getCitizen().getUser().getUsername();
            if (!ownerUsername.equals(currentUsername)) {
                throw new UnauthorizedActionException(
                        "You are not allowed to add documents to this service request.");
            }
        }

        // Resolve document reference — system-generated or caller-supplied
        String documentReference = resolveDocumentReference(request.getDocumentReference());

        SupportingDocument document = SupportingDocument.builder()
                .documentReference(documentReference)
                .serviceRequest(serviceRequest)
                .documentType(request.getDocumentType().trim())
                .documentName(request.getDocumentName().trim())
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();

        document = documentRepository.save(document);
        return mapToResponse(document);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the full metadata for a single document by its reference.
     *
     * @param documentReference the unique document reference (e.g. {@code DOC-8F3A91B2})
     * @return full document response
     * @throws ResourceNotFoundException if no document exists with the given reference
     */
    @Transactional(readOnly = true)
    public SupportingDocumentResponse getDocumentByReference(String documentReference) {
        SupportingDocument document = findDocumentOrThrow(documentReference);
        return mapToResponse(document);
    }

    /**
     * Returns all document metadata records attached to a given service request.
     *
     * <p>Returns an empty list (not 404) if the service request exists but has
     * no documents yet. Returns 404 if the service request itself does not exist.</p>
     *
     * @param requestReference the service request reference
     * @return list of document summary responses (may be empty)
     * @throws ResourceNotFoundException if the service request does not exist
     */
    @Transactional(readOnly = true)
    public List<SupportingDocumentSummaryResponse> getDocumentsByServiceRequest(String requestReference) {
        // Validate service request existence first
        if (!serviceRequestRepository.findByRequestReference(requestReference).isPresent()) {
            throw new ResourceNotFoundException("Service request not found: " + requestReference);
        }

        return documentRepository
                .findByServiceRequest_RequestReference(requestReference)
                .stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Updates the {@code documentType} and {@code documentName} of an existing document.
     *
     * @param documentReference the document to update
     * @param request           the update payload
     * @return updated full document response
     * @throws ResourceNotFoundException if no document exists with the given reference
     */
    @Transactional
    public SupportingDocumentResponse updateDocument(
            String documentReference, UpdateSupportingDocumentRequest request) {

        SupportingDocument document = findDocumentOrThrow(documentReference);
        document.setDocumentType(request.getDocumentType().trim());
        document.setDocumentName(request.getDocumentName().trim());
        document = documentRepository.save(document);
        return mapToResponse(document);
    }

    /**
     * Updates the verification status of a supporting document.
     *
     * <p>The {@code remarks} field in the request is informational — it is included
     * in the response echo but not persisted on the document entity. If remark
     * persistence is required in future, add a {@code verificationRemarks} column
     * to the entity and migration.</p>
     *
     * @param documentReference the document to verify/reject
     * @param request           the verification status payload
     * @return updated full document response
     * @throws ResourceNotFoundException if no document exists with the given reference
     */
    @Transactional
    public SupportingDocumentResponse updateVerificationStatus(
            String documentReference, UpdateDocumentVerificationStatusRequest request) {

        SupportingDocument document = findDocumentOrThrow(documentReference);

        if (document.getServiceRequest().getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new InvalidStatusException(
                    "Cannot verify documents on a cancelled service request.");
        }

        document.setVerificationStatus(request.getVerificationStatus());
        document = documentRepository.save(document);
        return mapToResponse(document);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Physically deletes a supporting document metadata record.
     *
     * <p>A hard delete is used here because only metadata is stored — there is no
     * binary asset to preserve. Deleted records are not recoverable, but their
     * parent service request and citizen data are unaffected.</p>
     *
     * @param documentReference the document to delete
     * @throws ResourceNotFoundException if no document exists with the given reference
     */
    @Transactional
    public void deleteDocument(String documentReference) {
        SupportingDocument document = findDocumentOrThrow(documentReference);
        documentRepository.delete(document);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ServiceRequest findServiceRequestOrThrow(String requestReference) {
        return serviceRequestRepository.findByRequestReference(requestReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found: " + requestReference));
    }

    private SupportingDocument findDocumentOrThrow(String documentReference) {
        return documentRepository.findByDocumentReference(documentReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supporting document not found: " + documentReference));
    }

    /**
     * Returns a unique document reference.
     * If {@code requested} is non-blank, validates its uniqueness and returns it.
     * Otherwise generates one via {@link ReferenceGenerator}.
     */
    private String resolveDocumentReference(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (documentRepository.existsByDocumentReference(requested)) {
                throw new DuplicateResourceException(
                        "Document reference already exists: " + requested);
            }
            return requested;
        }

        // System-generated — retry up to 10 times on unlikely collision
        String ref;
        int attempts = 0;
        do {
            ref = ReferenceGenerator.generateDocumentReference();
            if (++attempts > 10) {
                throw new BusinessException("Failed to generate a unique document reference. Please retry.");
            }
        } while (documentRepository.existsByDocumentReference(ref));
        return ref;
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private SupportingDocumentResponse mapToResponse(SupportingDocument document) {
        return SupportingDocumentResponse.builder()
                .documentReference(document.getDocumentReference())
                .requestReference(document.getServiceRequest().getRequestReference())
                .documentType(document.getDocumentType())
                .documentName(document.getDocumentName())
                .verificationStatus(document.getVerificationStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private SupportingDocumentSummaryResponse mapToSummaryResponse(SupportingDocument document) {
        return SupportingDocumentSummaryResponse.builder()
                .documentReference(document.getDocumentReference())
                .documentType(document.getDocumentType())
                .documentName(document.getDocumentName())
                .verificationStatus(document.getVerificationStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
