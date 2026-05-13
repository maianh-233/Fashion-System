package com.fashion.promotionservice.entity;

import com.fashion.promotionservice.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal minOrderValue;

    @Column(precision = 14, scale = 2)
    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usagePerUser;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PromotionUsage> usages = new HashSet<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PromotionCondition> conditions = new HashSet<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}