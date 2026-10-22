package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của OrderStatusHistory giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDto {
    private UUID id;
    private UUID orderId;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private UUID changedBy;
    private String note;
    private LocalDateTime changedAt;
    private OrderActorType actorType;
    private String reason;
}

