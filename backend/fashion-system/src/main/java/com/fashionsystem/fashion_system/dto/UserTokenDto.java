package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của UserToken giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenDto {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private String tokenType;
    private UUID refreshTokenFamily;
    private UUID parentTokenId;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String device;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}

