package com.fashion.inventoryservice.entity;

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
@Table(name = "goods_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssue extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "issue_code", nullable = false, unique = true)
    private String issueCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    private String status;

    private String note;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<GoodsIssueItem> items;
}
