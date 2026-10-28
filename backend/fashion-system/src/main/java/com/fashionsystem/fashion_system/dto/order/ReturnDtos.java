package com.fashionsystem.fashion_system.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Đổi / trả hàng: yêu cầu của khách, duyệt và nhận hàng tại cửa hàng. */
public final class ReturnDtos {
    public static final int WINDOW_DAYS = 14;

    /** Lý do khách chọn khi gửi yêu cầu, theo thứ tự hiển thị. */
    public static final List<Reason> REASONS = List.of(
            new Reason("WRONG_SIZE", "Không vừa size"),
            new Reason("DEFECTIVE", "Sản phẩm lỗi / hư hỏng"),
            new Reason("NOT_AS_DESCRIBED", "Không giống mô tả"),
            new Reason("WRONG_ITEM", "Giao sai sản phẩm"),
            new Reason("CHANGED_MIND", "Không còn nhu cầu"),
            new Reason("OTHER", "Lý do khác"));

    public static String reasonLabel(String code) {
        return REASONS.stream().filter(r -> r.code().equals(code)).map(Reason::label).findFirst().orElse(code);
    }

    public static final Map<String, String> STATUS_LABELS = Map.of(
            "REQUESTED", "Chờ cửa hàng duyệt",
            "APPROVED", "Đã duyệt · mang hàng tới cửa hàng",
            "REJECTED", "Bị từ chối",
            "CANCELLED", "Đã hủy",
            "COMPLETED", "Hoàn tất");

    private ReturnDtos() {
    }

    public record Reason(String code, String label) {
    }

    /** Phân loại khác cùng sản phẩm để đổi; {@code inStock} theo tồn tại quầy của cửa hàng đã bán. */
    public record ExchangeOption(UUID variantId, String color, String size, boolean inStock) {
    }

    /** Một dòng hàng của đơn và số lượng còn có thể đổi / trả. */
    public record EligibleItem(UUID orderItemId, UUID productId, UUID variantId, String productName, String color,
                               String size, String imageUrl, int purchased, int requestable, BigDecimal unitPrice,
                               List<ExchangeOption> exchangeOptions) {
    }

    /** Đơn còn trong hạn đổi / trả hay không, và các dòng hàng có thể yêu cầu. */
    public record Eligibility(UUID orderId, String orderCode, boolean eligible, String reason, LocalDateTime deadline,
                              List<EligibleItem> items, List<Reason> reasons) {
    }

    public record CreateItem(@NotNull UUID orderItemId, @NotNull @Min(1) Integer quantity, UUID exchangeVariantId) {
    }

    public record CreateRequest(
            @NotNull UUID orderId,
            @NotNull @Pattern(regexp = "RETURN|EXCHANGE") String type,
            @NotBlank String reasonCode,
            @Size(max = 1000) String note,
            @NotEmpty @Size(max = 50) List<@Valid @NotNull CreateItem> items) {
    }

    public record RequestItem(UUID id, UUID orderItemId, String productName, String color, String size, String imageUrl,
                              int quantity, BigDecimal unitPrice, UUID exchangeVariantId, String exchangeColor,
                              String exchangeSize, String condition) {
    }

    /** Yêu cầu đổi / trả (khách và nhân viên dùng chung). */
    public record ReturnRequestView(UUID id, String requestCode, UUID orderId, String orderCode, UUID storeId,
                                    String storeName, UUID customerId, String customerName, String customerPhone,
                                    String type, String status, String statusLabel, String reasonCode,
                                    String reasonLabel, String note, String rejectReason, BigDecimal estimatedRefund,
                                    BigDecimal refundAmount, String paymentMethod, String refundStatus,
                                    List<RequestItem> items, LocalDateTime createdAt, LocalDateTime approvedAt,
                                    LocalDateTime completedAt) {
    }

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    /** Tình trạng từng món khi nhận: GOOD → tồn tại quầy, DAMAGED → tồn hỏng. */
    public record CompleteRequest(@NotEmpty Map<UUID, @Pattern(regexp = "GOOD|DAMAGED") String> conditions) {
    }

    public record ConfirmRefundRequest(@Size(max = 100) String transactionCode) {
    }
}
