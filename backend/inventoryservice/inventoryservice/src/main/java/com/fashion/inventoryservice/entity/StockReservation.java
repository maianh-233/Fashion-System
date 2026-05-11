package com.fashion.inventoryservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.inventoryservice.entity.base.BaseEntity;

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
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    private Integer quantity;

    private String status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
}
