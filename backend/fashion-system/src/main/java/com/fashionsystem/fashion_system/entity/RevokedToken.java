package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JWT đã logout; lưu jti thay vì lưu token thô. */
@Entity
@Table(name = "revoked_tokens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RevokedToken {
    @Id @Column(name = "token_id", nullable = false)
    private UUID tokenId;
    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;
}
