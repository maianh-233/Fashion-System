package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_refresh_tokens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerRefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "refresh_token_family", nullable = false)
    private UUID refreshTokenFamily;
    @Column(name = "parent_token_id")
    private UUID parentTokenId;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "device", length = 100)
    private String device;
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

