package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

/**
 * DTO dùng để truyền dữ liệu của PermissionGroup giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupDto {
    private UUID id;
    @NotNull private UUID moduleId;
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 50) private String code;
    private String description;
    private LocalDateTime createdAt;
}
