package com.govtech.platform.statushistory.entity;

import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record of a status transition on a service request.
 *
 * <p>A new {@link StatusHistory} row is appended every time a service agent or
 * admin changes the {@link ServiceRequest} status. Records are never updated
 * or deleted — they form a tamper-evident chronological trail.</p>
 *
 * <p>{@code oldStatus} is nullable to support the initial SUBMITTED entry where
 * there is no prior state. {@code changedBy} stores the username of the actor.</p>
 */
@Entity
@Table(name = "status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    /** The status before this transition. {@code null} for the initial SUBMITTED entry. */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ServiceRequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceRequestStatus newStatus;

    /** Username of the user who performed the status change. */
    @Column(length = 150)
    private String changedBy;

    /** Immutable timestamp of the transition. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /** Optional notes explaining the reason for the transition. */
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
