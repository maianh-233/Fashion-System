package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một dòng hàng trong yêu cầu đổi / trả (bảng {@code return_request_items}). */
@Entity
@Table(name = "return_request_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    /** Biến thể khách trả lại. */
    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Giá thực trả cho một món (tổng dòng / số lượng), dùng để tính tiền hoàn. */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** Phân loại khách muốn đổi sang (chỉ yêu cầu đổi hàng). */
    @Column(name = "exchange_variant_id")
    private UUID exchangeVariantId;

    /** GOOD | DAMAGED, nhân viên chọn khi nhận hàng. */
    @Column(name = "item_condition", length = 20)
    private String itemCondition;
}
