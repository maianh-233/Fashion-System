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
 * Entity đại diện cho bảng {@code goods_issues}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "goods_issues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsIssue {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu giá trị issue code của bản ghi. */
    @Column(name = "issue_code", nullable = false, unique = true, length = 50)
    private String issueCode;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id")
    private UUID orderId;

    /** Lưu giá trị issued by của bản ghi. */
    @Column(name = "issued_by")
    private UUID issuedBy;

    /** Lưu giá trị approved by của bản ghi. */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /** Lưu giá trị issue type của bản ghi. */
    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;

    /** Lưu giá trị issue date của bản ghi. */
    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Lưu giá trị total quantity của bản ghi. */
    @Column(name = "total_quantity")
    private Integer totalQuantity;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

