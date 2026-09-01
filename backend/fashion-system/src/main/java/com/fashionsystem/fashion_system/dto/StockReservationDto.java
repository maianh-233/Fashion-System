package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của StockReservation giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationDto {
    private UUID id;
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID storeId;
    @NotNull
    private UUID productVariantId;
    @NotNull @Min(1)
    private Integer quantity;
    private String status;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
