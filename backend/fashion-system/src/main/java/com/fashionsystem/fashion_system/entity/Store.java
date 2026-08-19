package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code stores}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "stores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã nghiệp vụ của bản ghi. */
    @Column(name = "code", unique = true, length = 50)
    private String code;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu giá trị address của bản ghi. */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** Lưu số điện thoại của bản ghi. */
    @Column(name = "phone", length = 20)
    private String phone;

    /** Lưu giá trị latitude của bản ghi. */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Lưu giá trị longitude của bản ghi. */
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

