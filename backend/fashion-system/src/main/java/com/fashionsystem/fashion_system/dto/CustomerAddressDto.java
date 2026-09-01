package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của CustomerAddress giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressDto {
    private UUID id;
    private UUID customerId;
    @NotBlank @Size(max = 255)
    private String receiverName;
    @NotBlank @Size(max = 20)
    private String receiverPhone;
    @Size(max = 100)
    private String province;
    @Size(max = 100)
    private String district;
    @Size(max = 100)
    private String ward;
    @NotBlank
    private String addressLine;
    @Size(max = 20)
    private String postalCode;
    @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal latitude;
    @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal longitude;
    private Boolean isDefault;
    @Pattern(regexp = "HOME|WORK|OTHER")
    private String addressType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
