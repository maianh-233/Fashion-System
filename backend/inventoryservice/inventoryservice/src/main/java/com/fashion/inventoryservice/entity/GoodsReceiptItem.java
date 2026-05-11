package com.fashion.inventoryservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goods_receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private GoodsReceipt receipt;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    private String sku;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    private Integer quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
