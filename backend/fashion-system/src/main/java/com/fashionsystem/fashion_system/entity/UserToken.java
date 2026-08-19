package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code user_tokens}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu giá trị token hash của bản ghi. */
    @Column(name = "token_hash", nullable = false, columnDefinition = "TEXT")
    private String tokenHash;

    /** Lưu giá trị token type của bản ghi. */
    @Column(name = "token_type", length = 30)
    private String tokenType;

    /** Lưu giá trị refresh token family của bản ghi. */
    @Column(name = "refresh_token_family")
    private UUID refreshTokenFamily;

    /** Lưu mã tham chiếu đến parent token. */
    @Column(name = "parent_token_id")
    private UUID parentTokenId;

    /** Lưu thời điểm hết hạn của bản ghi. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Lưu giá trị revoked at của bản ghi. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Lưu giá trị device của bản ghi. */
    @Column(name = "device", length = 100)
    private String device;

    /** Lưu giá trị ip address của bản ghi. */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** Lưu giá trị user agent của bản ghi. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

