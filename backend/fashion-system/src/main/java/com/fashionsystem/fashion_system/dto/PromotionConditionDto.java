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
 * DTO dùng để truyền dữ liệu của PromotionCondition giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionConditionDto {
    private UUID id;
    private UUID promotionId;
    @NotBlank @Size(max = 50)
    private String conditionType;
    @NotBlank @Size(max = 255)
    private String conditionValue;
    private LocalDateTime createdAt;
}
