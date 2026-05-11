package com.fashion.orderservice.entity;

import com.fashion.orderservice.entity.base.BaseEntity;
import com.fashion.orderservice.entity.enums.OrderStatus;
import com.fashion.orderservice.entity.enums.OrderType;
import com.fashion.orderservice.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", precision = 14, scale = 2)
    private BigDecimal discountTotal;

    @Column(precision = 14, scale = 2)
    private BigDecimal tax;

    @Column(name = "shipping_fee", precision = 14, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private OrderAddress address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Shipment shipment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderPromotion> promotions = new ArrayList<>();
}