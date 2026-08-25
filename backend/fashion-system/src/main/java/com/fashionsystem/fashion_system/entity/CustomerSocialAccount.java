package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Liên kết một khách hàng với định danh ổn định của Google hoặc Facebook. */
@Entity
@Table(name = "customer_social_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_customer_social_provider_subject", columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(name = "uq_customer_social_customer_provider", columnNames = {"customer_id", "provider"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerSocialAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;
    @Column(name = "provider_email", length = 255)
    private String providerEmail;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
