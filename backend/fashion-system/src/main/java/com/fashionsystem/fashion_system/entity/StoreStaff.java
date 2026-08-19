package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code store_staffs}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "store_staffs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStaff {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Lưu giá trị staff role của bản ghi. */
    @Column(name = "staff_role", length = 50)
    private String staffRole;

    /** Lưu giá trị start date của bản ghi. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Lưu giá trị end date của bản ghi. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

