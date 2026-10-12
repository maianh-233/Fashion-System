package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.promotion.CustomerPromotionResponse;
import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.LoyaltyAccount;
import com.fashionsystem.fashion_system.entity.LoyaltyTransaction;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionMapper;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyAccountRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyTransactionRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionTierRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khách hàng dùng điểm tích lũy để đổi khuyến mãi loại POINT_REDEMPTION. Mỗi lần đổi trừ điểm, ghi giao dịch
 * REDEEM vào sổ điểm và lưu liên kết khách hàng - khuyến mãi; khách được đổi không giới hạn số lần khi đủ điểm.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionRedemptionService {
    static final String REDEEM_TRANSACTION = "REDEEM";
    static final String REFERENCE_TYPE = "PROMOTION_REDEMPTION";

    private final PromotionRepository promotionRepository;
    private final PromotionTierRepository promotionTierRepository;
    private final CustomerTierRepository tierRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PromotionMapper mapper;

    /** Các khuyến mãi đổi điểm đang hoạt động mà hạng hiện tại của khách được đổi. */
    public Page<PromotionDto> redeemable(UUID customerId, String keyword, Pageable pageable) {
        String normalized = keyword == null ? "" : keyword.trim();
        UUID tierId = tierRepository.findCurrentByCustomerId(customerId).map(CustomerTier::getId).orElse(null);
        return promotionRepository.findRedeemable(normalized, tierId, pageable).map(mapper::toDto);
    }

    /** Khuyến mãi khách hàng đã đổi, mới nhất trước. */
    public Page<CustomerPromotionResponse> mine(UUID customerId, Pageable pageable) {
        return customerPromotionRepository.findAllByCustomerId(customerId, pageable).map(row ->
                new CustomerPromotionResponse(row.getId(), row.getRedeemedAt(), row.getPointsSpent(), null,
                        promotionRepository.findById(row.getPromotionId()).map(mapper::toDto).orElse(null)));
    }

    /** Đổi điểm lấy khuyến mãi. */
    @Transactional
    @com.fashionsystem.fashion_system.audit.AuditInfrastructure(
            reason = "Customer self-service; recorded in the loyalty_transactions ledger")
    @CacheEvict(cacheNames = CacheNames.PROMOTION_DETAIL, key = "#promotionId")
    public CustomerPromotionResponse redeem(UUID customerId, UUID promotionId) {
        LocalDateTime now = PromotionService.now();
        Promotion promotion = promotionRepository.findByIdForUpdate(promotionId)
                .orElseThrow(() -> BusinessException.notFound("Khuyến mãi không tồn tại"));
        if (promotion.getPromotionType() != PromotionType.POINT_REDEMPTION)
            throw BusinessException.badRequest("Khuyến mãi này không dùng để đổi điểm");
        if (promotion.getStatus() != PromotionStatus.ACTIVE
                || now.isBefore(promotion.getStartDate()) || !now.isBefore(promotion.getEndDate()))
            throw BusinessException.invalidState("Khuyến mãi không trong thời gian đổi điểm");
        if (promotionTierRepository.existsByPromotionId(promotionId)) {
            UUID tierId = tierRepository.findCurrentByCustomerId(customerId).map(CustomerTier::getId).orElse(null);
            if (tierId == null || !promotionTierRepository.existsByPromotionIdAndTierId(promotionId, tierId))
                throw BusinessException.forbidden("Hạng thành viên của bạn không được đổi khuyến mãi này");
        }
        if (promotion.getUsageLimit() != null
                && customerPromotionRepository.countByPromotionId(promotionId) >= promotion.getUsageLimit())
            throw BusinessException.invalidState("Khuyến mãi đã hết lượt đổi");

        int points = promotion.getPointsRequired();
        LoyaltyAccount account = loyaltyAccountRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.invalidState("Bạn chưa có điểm tích lũy"));
        int balance = account.getPointsBalance() == null ? 0 : account.getPointsBalance();
        if (balance < points)
            throw BusinessException.invalidState("Bạn không đủ điểm để đổi khuyến mãi này (cần "
                    + points + " điểm, hiện có " + balance + " điểm)");
        account.setPointsBalance(balance - points);
        account.setUpdatedAt(now);
        loyaltyAccountRepository.save(account);

        CustomerPromotion redeemed = customerPromotionRepository.save(CustomerPromotion.builder()
                .customerId(customerId).promotionId(promotionId).pointsSpent(points).redeemedAt(now).build());
        loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                .customerId(customerId).transactionType(REDEEM_TRANSACTION).points(-points)
                .referenceType(REFERENCE_TYPE).referenceId(redeemed.getId())
                .note("Đổi khuyến mãi " + promotion.getCode()).createdAt(now).build());
        return new CustomerPromotionResponse(redeemed.getId(), now, points, account.getPointsBalance(),
                mapper.toDto(promotion));
    }
}
