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
@Table(name = "customer_password_reset_challenges")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerPasswordResetChallenge {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "customer_account_id", nullable = false)
    private UUID customerAccountId;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(name = "otp_hash", nullable = false, columnDefinition = "TEXT")
    private String otpHash;
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

