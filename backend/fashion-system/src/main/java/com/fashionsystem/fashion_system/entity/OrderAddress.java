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
 * Entity đại diện cho bảng {@code order_addresses}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddress {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã đơn hàng của bản ghi. */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** Lưu giá trị receiver name của bản ghi. */
    @Column(name = "receiver_name", nullable = false, length = 255)
    private String receiverName;

    /** Lưu giá trị receiver phone của bản ghi. */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    /** Lưu giá trị province của bản ghi. */
    @Column(name = "province", length = 100)
    private String province;

    /** Lưu giá trị district của bản ghi. */
    @Column(name = "district", length = 100)
    private String district;

    /** Lưu giá trị ward của bản ghi. */
    @Column(name = "ward", length = 100)
    private String ward;

    /** Lưu giá trị address line của bản ghi. */
    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    /** Lưu giá trị postal code của bản ghi. */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /** Lưu giá trị latitude của bản ghi. */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Lưu giá trị longitude của bản ghi. */
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Lưu giá trị address type của bản ghi. */
    @Column(name = "address_type", length = 20)
    private String addressType;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

