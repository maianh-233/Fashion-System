package com.fashion.inventoryservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.inventoryservice.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "receipt_code", nullable = false, unique = true)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;

    private String status;

    private String note;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY)
    private List<GoodsReceiptItem> items;
}