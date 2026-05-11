package com.fashion.inventoryservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(InventoryBalanceId.class)
public class InventoryBalance {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Id
    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;

    @Column(name = "damaged_quantity")
    private Integer damagedQuantity;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}