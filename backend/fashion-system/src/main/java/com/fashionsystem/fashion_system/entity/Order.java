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
 * Entity đại diện cho bảng {@code orders}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu giá trị order code của bản ghi. */
    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    /** Lưu mã khách hàng đặt đơn; không tham chiếu bảng users. */
    @Column(name = "customer_id")
    private UUID customerId;

    /** Lưu mã cửa hàng của bản ghi. */
    @Column(name = "store_id")
    private UUID storeId;

    /** Lưu giá trị order type của bản ghi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    /** Lưu giá trị subtotal của bản ghi. */
    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Lưu giá trị discount total của bản ghi. */
    @Column(name = "discount_total", precision = 14, scale = 2)
    private BigDecimal discountTotal;

    /** Lưu giá trị tax của bản ghi. */
    @Column(name = "tax", precision = 14, scale = 2)
    private BigDecimal tax;

    /** Lưu giá trị shipping fee của bản ghi. */
    @Column(name = "shipping_fee", precision = 14, scale = 2)
    private BigDecimal shippingFee;

    /** Lưu giá trị total amount của bản ghi. */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** Lưu giá trị payment status của bản ghi. */
    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Đơn cha khi đơn online bị tách cho nhiều cửa hàng xử lý; null với đơn thường và đơn cha. */
    @Column(name = "parent_order_id")
    private UUID parentOrderId;

    /** Nhân viên đang xử lý đơn (người tạo đơn offline, người xác nhận đơn online). */
    @Column(name = "handler_user_id")
    private UUID handlerUserId;

    /** Nhân viên tạo đơn; null với đơn khách tự đặt. */
    @Column(name = "created_by")
    private UUID createdBy;

    /** Phương thức thanh toán chính của đơn (CASH, COD, VNPAY, MOMO...). */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    /** Tiền khách đưa với đơn thanh toán tiền mặt tại quầy. */
    @Column(name = "cash_received", precision = 14, scale = 2)
    private BigDecimal cashReceived;

    /** Tiền thối lại cho khách. */
    @Column(name = "change_amount", precision = 14, scale = 2)
    private BigDecimal changeAmount;

    /** Lý do hủy đơn (bắt buộc khi nhân viên hủy). */
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /** Bên đã hủy đơn. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by_type", length = 20)
    private OrderActorType cancelledByType;

    /** false khi đơn đã bị ẩn (xóa mềm) khỏi danh sách. */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "hidden_by")
    private UUID hiddenBy;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "restored_by")
    private UUID restoredBy;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    /** Khóa chống gửi trùng do trang thanh toán của khách sinh cho mỗi lần đặt hàng. */
    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    /** Hạn thanh toán của đơn trả qua cổng (VNPay/MoMo); quá hạn chưa trả thì hệ thống tự hủy. */
    @Column(name = "payment_due_at")
    private LocalDateTime paymentDueAt;

}
