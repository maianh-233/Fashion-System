package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Số tiền khách phải trả cho một đơn của cửa hàng.
 *
 * <p>Đơn thường: tổng tiền đơn. Phần của đơn bị tách: tổng phần đó (đã trừ giảm giá theo sản phẩm) trừ phần voucher
 * toàn đơn và cộng phần phí ship được chia, cả hai chia theo tỷ lệ giá trị các phần; số lẻ do làm tròn tới đồng dồn
 * vào phần cuối (theo mã đơn) để tổng các phần luôn bằng tổng đơn của khách. Dùng cho tiền COD tài xế thu và tiền
 * hoàn khi hủy một phần.</p>
 */
@Component
@RequiredArgsConstructor
public class OrderAmountCalculator {
    private final OrderRepository orderRepository;

    public BigDecimal amountDue(Order storeOrder) {
        if (storeOrder.getParentOrderId() == null) return orZero(storeOrder.getTotalAmount());
        Order parent = orderRepository.findById(storeOrder.getParentOrderId()).orElseThrow();
        List<Order> parts = orderRepository.findAllByParentOrderId(parent.getId()).stream()
                .sorted(Comparator.comparing(Order::getOrderCode)).toList();
        List<BigDecimal> weights = parts.stream().map(o -> orZero(o.getTotalAmount())).toList();
        BigDecimal productDiscounts = parts.stream().map(o -> orZero(o.getDiscountTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderDiscount = orZero(parent.getDiscountTotal()).subtract(productDiscounts).max(BigDecimal.ZERO);
        List<BigDecimal> discountShares = allocate(orderDiscount, weights);
        List<BigDecimal> shippingShares = allocate(orZero(parent.getShippingFee()), weights);
        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).getId().equals(storeOrder.getId()))
                return weights.get(i).subtract(discountShares.get(i)).add(shippingShares.get(i)).max(BigDecimal.ZERO);
        throw new IllegalStateException("Order is not a part of its parent");
    }

    /** Chia {@code amount} theo tỷ lệ {@code weights}, làm tròn tới đồng, phần dư vào phần tử cuối. */
    static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal totalWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < weights.size(); i++) {
            BigDecimal share;
            if (i == weights.size() - 1) share = amount.subtract(assigned);
            else if (totalWeight.signum() == 0) share = BigDecimal.ZERO;
            else share = amount.multiply(weights.get(i)).divide(totalWeight, 0, RoundingMode.HALF_UP);
            shares.add(share.setScale(2, RoundingMode.HALF_UP));
            assigned = assigned.add(share);
        }
        return shares;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
