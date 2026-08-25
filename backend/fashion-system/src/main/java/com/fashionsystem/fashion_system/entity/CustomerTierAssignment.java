package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lịch sử gán hạng cho khách hàng. */
@Entity
@Table(name = "customer_tier_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerTierAssignment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "tier_id", nullable = false)
    private UUID tierId;
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
