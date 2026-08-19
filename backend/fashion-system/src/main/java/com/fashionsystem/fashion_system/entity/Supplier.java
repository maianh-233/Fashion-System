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
 * Entity đại diện cho bảng {@code suppliers}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
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

    /** Lưu giá trị contact name của bản ghi. */
    @Column(name = "contact_name", length = 255)
    private String contactName;

    /** Lưu số điện thoại của bản ghi. */
    @Column(name = "phone", length = 20)
    private String phone;

    /** Lưu địa chỉ email của bản ghi. */
    @Column(name = "email", length = 255)
    private String email;

    /** Lưu giá trị address của bản ghi. */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 50)
    private String status;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

