package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của RolePermissionAudit giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionAuditDto {
    private UUID id;
    private UUID roleId;
    private UUID permissionId;
    private String action;
    private UUID changedBy;
    private LocalDateTime changedAt;
}

