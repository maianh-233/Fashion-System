package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code order_chat_rooms}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChatRoom {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** Lưu mã tham chiếu đến customer. */
    @Column(name = "customer_id")
    private UUID customerId;

    /** Lưu mã tham chiếu đến assigned staff. */
    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu giá trị last message at của bản ghi. */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Lưu giá trị closed at của bản ghi. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

}

