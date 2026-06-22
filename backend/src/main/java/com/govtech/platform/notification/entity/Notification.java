package com.govtech.platform.notification.entity;

import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.common.enums.NotificationStatus;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * In-app notification delivered to a citizen when their service request status changes.
 *
 * <p>Notifications are immutable once created — only the {@code status} field
 * (UNREAD → READ) is updated when the citizen acknowledges them.</p>
 *
 * <p>There is intentionally no {@code updatedAt} field because the only
 * meaningful update is the status transition, which is captured by querying
 * {@code status} directly.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.UNREAD;

    /** Immutable creation timestamp. No {@code updatedAt} — see class javadoc. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = NotificationStatus.UNREAD;
        }
    }
}
