package com.govtech.platform.document.repository;

import com.govtech.platform.document.entity.SupportingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link SupportingDocument} entities.
 */
@Repository
public interface SupportingDocumentRepository extends JpaRepository<SupportingDocument, Long> {

    /**
     * Finds a document by its system-generated unique reference.
     */
    Optional<SupportingDocument> findByDocumentReference(String documentReference);

    /**
     * Returns all documents attached to a specific service request.
     * Used when rendering the document list for a given request.
     */
    List<SupportingDocument> findByServiceRequest_RequestReference(String requestReference);

    /** Returns {@code true} if a document with the given reference already exists. */
    boolean existsByDocumentReference(String documentReference);
}
