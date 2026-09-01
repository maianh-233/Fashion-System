package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Supplier giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private UUID id;
    @Size(max = 50)
    private String code;
    @NotBlank @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String contactName;
    @Size(max = 20)
    private String phone;
    @Email @Size(max = 255)
    private String email;
    private String address;
    @Size(max = 50)
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
