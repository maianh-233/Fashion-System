package com.fashion.authservice.entity;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_tokens")
@Getter @Setter
public class UserToken {

    @Id
    @GeneratedValue
    private UUID id;

    private String tokenHash;

    private String tokenType = "REFRESH";

    private UUID refreshTokenFamily;

    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    private String device;
    private String ipAddress;
    private String userAgent;

    private LocalDateTime createdAt;

    // ================= RELATION =================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_token_id")
    private UserToken parentToken;
}
