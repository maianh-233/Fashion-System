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
 * Entity đại diện cho bảng {@code order_status_histories}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_status_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Lưu giá trị from status của bản ghi. */
    @Column(name = "from_status", length = 50)
    private String fromStatus;

    /** Lưu giá trị to status của bản ghi. */
    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    /** Lưu giá trị changed by của bản ghi. */
    @Column(name = "changed_by")
    private UUID changedBy;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Lưu giá trị changed at của bản ghi. */
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

}

