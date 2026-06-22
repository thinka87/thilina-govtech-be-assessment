package com.govtech.platform.document.entity;

import com.govtech.platform.common.enums.DocumentVerificationStatus;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Metadata record for a supporting document attached to a service request.
 *
 * <p>For the purposes of this assessment, only document metadata is stored
 * (type, name, verification status). Actual binary file storage (S3, local FS)
 * is outside scope but can be added by persisting a storage path or URL field.</p>
 *
 * <p>A service agent updates {@code verificationStatus} from PENDING to
 * VERIFIED or REJECTED after reviewing the physical/digital document.</p>
 */
@Entity
@Table(name = "supporting_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System-generated unique reference. e.g. {@code DOC-20240001}. */
    @Column(nullable = false, unique = true, length = 100)
    private String documentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    /** Category of document, e.g. {@code NATIONAL_ID}, {@code PROOF_OF_ADDRESS}. */
    @Column(nullable = false, length = 100)
    private String documentType;

    /** Original file name or descriptive label provided by the citizen. */
    @Column(nullable = false, length = 200)
    private String documentName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.verificationStatus == null) {
            this.verificationStatus = DocumentVerificationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
