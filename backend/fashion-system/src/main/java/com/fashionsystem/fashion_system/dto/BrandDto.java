package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Brand giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {
    private UUID id;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 100)
    private String code;
    private String logo;
    private String description;
    @Size(max = 50)
    private String status;
    private LocalDateTime terminatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
