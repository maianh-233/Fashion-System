
package com.fashionsystem.fashion_system.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code stores}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "stores", uniqueConstraints = {
        @UniqueConstraint(name = "uq_stores_coordinates", columnNames = {"latitude", "longitude"})
})
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
    @Column(name = "code", nullable = false, unique = true, length = 50, updatable = false)
    private String code;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu giá trị address của bản ghi. */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** Lưu số điện thoại của bản ghi. */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** Lưu giá trị latitude của bản ghi. */
    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Lưu giá trị longitude của bản ghi. */
    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Giờ mở cửa hiển thị cho khách, ví dụ "09:00 – 22:00". */
    @Column(name = "opening_hours", length = 100)
    private String openingHours;

    /** Ảnh cửa hàng (URL trên kho ảnh). */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /** Mã ảnh trên kho ảnh, để xóa ảnh cũ khi thay. */
    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;

    /** Dịch vụ tại cửa hàng hiển thị cho khách, ví dụ "Styling riêng". */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "services", columnDefinition = "text[]", nullable = false)
    private List<String> services = new ArrayList<>();

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
