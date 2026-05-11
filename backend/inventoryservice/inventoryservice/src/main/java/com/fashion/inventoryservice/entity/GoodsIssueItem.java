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
@Table(name = "goods_issue_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private GoodsIssue issue;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    private String sku;

    @Column(name = "product_name")
    private String productName;

    private Integer quantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
