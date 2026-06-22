package com.govtech.platform.servicerequest.entity;

import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.common.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a citizen's formal request for a government service.
 *
 * <p>Each request is assigned a unique {@code requestReference} used in API paths
 * and cross-module references (e.g. notifications, status history, documents).
 * Status transitions are recorded in the {@code status_history} table.</p>
 *
 * <p>The relationship to {@link Citizen} is many-to-one and unidirectional to
 * prevent JSON cycles. Documents, notifications, and status history reference
 * this entity via foreign key.</p>
 */
@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System-generated unique reference. e.g. {@code SR-20240001}. */
    @Column(nullable = false, unique = true, length = 100)
    private String requestReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    /** Category of service requested, e.g. {@code PASSPORT}, {@code BIRTH_CERTIFICATE}. */
    @Column(nullable = false, length = 100)
    private String serviceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceRequestStatus status = ServiceRequestStatus.SUBMITTED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ServiceRequestStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
