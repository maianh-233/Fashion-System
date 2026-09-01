package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của OrderAddress giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddressDto {
    private UUID id;
    private UUID orderId;
    @NotBlank @Size(max = 255)
    private String receiverName;
    @NotBlank @Size(max = 20)
    private String receiverPhone;
    private String province;
    private String district;
    private String ward;
    private String addressLine;
    private String postalCode;
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal longitude;
    private String addressType;
    private LocalDateTime createdAt;
}
