package com.fashion.inventoryservice.entity;

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
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    private Integer quantity;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
