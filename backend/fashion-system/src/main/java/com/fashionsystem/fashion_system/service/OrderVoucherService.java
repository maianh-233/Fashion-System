package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.PromotionProductRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionTierRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều kiện dùng voucher khi bán hàng, theo đúng quy tắc của module khuyến mãi:
 * thời gian hiệu lực, hạng thành viên, tổng lượt dùng, lượt dùng mỗi khách và voucher đổi điểm
 * (khách phải sở hữu một voucher chưa dùng). Khuyến mãi không giới hạn theo cửa hàng.
 *
 * <p>Điều kiện phụ thuộc giỏ hàng (đơn tối thiểu, sản phẩm áp dụng) được xét khi tính tiền.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderVoucherService {
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionTierRepository promotionTierRepository;
    private final PromotionUsageRepository usageRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final CustomerTierRepository tierRepository;

    /** Khách của đơn: null với khách vãng lai. */
    public record CustomerContext(UUID customerId, UUID tierId, Set<UUID> ownedVoucherIds) {
        public static CustomerContext walkIn() {
            return new CustomerContext(null, null, Set.of());
        }

        public boolean identified() {
            return customerId != null;
        }
    }

    /** Sản phẩm và hạng thành viên được áp dụng của từng khuyến mãi. */
    public record Targets(Map<UUID, Set<UUID>> productIds, Map<UUID, Set<UUID>> tierIds) {
        public Set<UUID> products(UUID promotionId) {
            return productIds.getOrDefault(promotionId, Set.of());
        }

        public Set<UUID> tiers(UUID promotionId) {
            return tierIds.getOrDefault(promotionId, Set.of());
        }
    }

    public CustomerContext customer(UUID customerId) {
        if (customerId == null) return CustomerContext.walkIn();
        UUID tierId = tierRepository.findCurrentByCustomerId(customerId).map(CustomerTier::getId).orElse(null);
        Set<UUID> owned = customerPromotionRepository.findAllByCustomerIdAndUsedOrderIdIsNull(customerId).stream()
                .map(CustomerPromotion::getPromotionId).collect(Collectors.toSet());
        return new CustomerContext(customerId, tierId, owned);
    }

    /**
     * Voucher hiển thị cho nhân viên: khuyến mãi toàn đơn và sản phẩm đang hiệu lực, cùng voucher
     * đổi điểm mà khách đang sở hữu.
     */
    public List<Promotion> listable(CustomerContext customer, LocalDateTime at) {
        Map<UUID, Promotion> result = new LinkedHashMap<>();
        promotionRepository.findEffective(at, List.of(PromotionType.ORDER, PromotionType.PRODUCT))
                .forEach(p -> result.put(p.getId(), p));
        if (!customer.ownedVoucherIds().isEmpty())
            promotionRepository.findAllById(customer.ownedVoucherIds()).forEach(p -> result.putIfAbsent(p.getId(), p));
        return new ArrayList<>(result.values());
    }

    public Targets targets(Collection<UUID> promotionIds) {
        if (promotionIds.isEmpty()) return new Targets(Map.of(), Map.of());
        Map<UUID, Set<UUID>> products = new HashMap<>();
        promotionProductRepository.findAllByPromotionIdIn(promotionIds).forEach(row ->
                products.computeIfAbsent(row.getPromotionId(), ignored -> new HashSet<>()).add(row.getProductId()));
        Map<UUID, Set<UUID>> tiers = new HashMap<>();
        promotionTierRepository.findAllByPromotionIdIn(promotionIds).forEach(row ->
                tiers.computeIfAbsent(row.getPromotionId(), ignored -> new HashSet<>()).add(row.getTierId()));
        return new Targets(products, tiers);
    }

    /**
     * Lý do voucher không dùng được cho khách này, hoặc null nếu dùng được. Không xét giỏ hàng.
     * Khi tạo đơn, gọi sau khi đã khóa dòng khuyến mãi để số lượt dùng không bị vượt khi bán đồng thời.
     */
    public String ineligibleReason(Promotion promotion, CustomerContext customer, Targets targets, LocalDateTime at) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE
                || at.isBefore(promotion.getStartDate()) || !at.isBefore(promotion.getEndDate()))
            return "Voucher không còn hiệu lực";
        Set<UUID> tiers = targets.tiers(promotion.getId());
        if (!tiers.isEmpty()) {
            if (!customer.identified()) return "Voucher chỉ dành cho khách thành viên, cần chọn khách hàng";
            if (customer.tierId() == null || !tiers.contains(customer.tierId()))
                return "Hạng thành viên của khách không được dùng voucher này";
        }
        if (promotion.getPromotionType() == PromotionType.POINT_REDEMPTION) {
            // Lượt của voucher đổi điểm đã được tính lúc khách đổi; khi bán chỉ cần khách còn voucher chưa dùng.
            if (!customer.identified()) return "Voucher đổi điểm cần chọn khách hàng";
            if (!customer.ownedVoucherIds().contains(promotion.getId())) return "Khách chưa đổi điểm lấy voucher này";
            return null;
        }
        if (promotion.getUsageLimit() != null
                && usageRepository.countByPromotionId(promotion.getId()) >= promotion.getUsageLimit())
            return "Voucher đã hết lượt sử dụng";
        if (promotion.getUsagePerUser() != null) {
            if (!customer.identified()) return "Voucher giới hạn lượt mỗi khách, cần chọn khách hàng";
            if (usageRepository.countByPromotionIdAndCustomerId(promotion.getId(), customer.customerId())
                    >= promotion.getUsagePerUser())
                return "Khách đã dùng hết lượt của voucher này";
        }
        return null;
    }

    public static boolean isOrderLevel(Promotion promotion) {
        return promotion.getPromotionType() != PromotionType.PRODUCT;
    }
}
