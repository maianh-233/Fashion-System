package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import com.fashionsystem.fashion_system.entity.LoyaltyAccount;
import com.fashionsystem.fashion_system.entity.LoyaltyTransaction;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyAccountRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyTransactionRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tích điểm theo đơn hàng và trừ lại khi hoàn tiền.
 *
 * <ul>
 *   <li>1.000đ tiền hàng khách trả (sau giảm giá, không gồm phí giao) = 1 điểm, làm tròn xuống.</li>
 *   <li>Cộng khi đơn cửa hàng hoàn tất: đơn online khi giao thành công (đơn tách thì từng phần), đơn tại quầy ngay khi
 *       thanh toán. Khách vãng lai không tích điểm. Mỗi đơn chỉ tích một lần.</li>
 *   <li>Hoàn tiền thành công: trừ điểm và tổng chi tiêu theo số tiền hoàn, không vượt phần đơn đó đã tích. Số dư không
 *       đủ thì trừ về 0 và ghi chú phần không trừ được.</li>
 *   <li>Tổng chi tiêu đạt mức của hạng cao hơn thì tự nâng hạng; hoàn tiền không hạ hạng.</li>
 * </ul>
 */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Loyalty points follow order completion and refunds; recorded in the loyalty_transactions ledger")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyEarningService {
    static final BigDecimal VND_PER_POINT = new BigDecimal("1000");
    static final String EARN = "EARN";
    static final String REFUND_REVERSAL = "REFUND_REVERSAL";
    static final String ORDER_REFERENCE = "ORDER";

    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTransactionRepository ledger;
    private final CustomerTierRepository tiers;
    private final CustomerTierAssignmentRepository assignments;
    private final OrderRepository orders;
    private final OrderAmountCalculator amounts;
    private final NamedParameterJdbcTemplate jdbc;

    static int pointsFor(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return 0;
        return amount.divide(VND_PER_POINT, 0, RoundingMode.DOWN).intValueExact();
    }

    /** Tích điểm cho đơn cửa hàng vừa hoàn tất; bỏ qua đơn tổng, khách vãng lai và đơn đã tích. */
    @Transactional
    public void earnForCompletedOrder(Order order) {
        if (order.getCustomerId() == null || order.getStoreId() == null || order.getStatus() != OrderStatus.COMPLETED)
            return;
        if (ledger.existsByTransactionTypeAndReferenceTypeAndReferenceId(EARN, ORDER_REFERENCE, order.getId())) return;
        BigDecimal amount = amounts.goodsAmount(order);
        int points = pointsFor(amount);
        LocalDateTime now = LocalDateTime.now();
        LoyaltyAccount account = lockAccount(order.getCustomerId());
        account.setPointsBalance(balance(account) + points);
        account.setTotalSpent(spent(account).add(amount));
        account.setUpdatedAt(now);
        accounts.save(account);
        ledger.save(LoyaltyTransaction.builder().customerId(order.getCustomerId()).transactionType(EARN)
                .points(points).amount(amount).referenceType(ORDER_REFERENCE).referenceId(order.getId())
                .note("Tích điểm đơn " + order.getOrderCode()).createdAt(now).build());
        upgradeTier(order.getCustomerId(), account.getTotalSpent(), now);
    }

    /**
     * Trừ điểm của khoản hoàn tiền vừa hoàn tất. Phạm vi là đơn của yêu cầu hoàn tiền (hoặc đơn của thanh toán) cùng
     * các đơn con; số điểm và số tiền trừ chia lần lượt cho từng đơn đã tích, không vượt phần còn lại của đơn đó.
     */
    @Transactional
    public void reverseForRefund(Refund refund, Payment payment) {
        UUID root = refund.getOrderId() != null ? refund.getOrderId() : payment.getOrderId();
        if (root == null || refund.getAmount() == null || refund.getAmount().signum() <= 0) return;
        Set<UUID> scope = new LinkedHashSet<>();
        scope.add(root);
        orders.findAllByParentOrderId(root).forEach(child -> scope.add(child.getId()));

        Map<UUID, List<LoyaltyTransaction>> byOrder = ledger.findAllByReferenceTypeAndReferenceIdIn(ORDER_REFERENCE, scope)
                .stream().collect(Collectors.groupingBy(LoyaltyTransaction::getReferenceId));
        List<LoyaltyTransaction> earned = byOrder.values().stream().flatMap(List::stream)
                .filter(row -> EARN.equals(row.getTransactionType()))
                .sorted(Comparator.comparing(LoyaltyTransaction::getCreatedAt).thenComparing(LoyaltyTransaction::getId))
                .toList();
        if (earned.isEmpty()) return;

        Map<UUID, String> codes = orders.findAllById(scope).stream()
                .collect(Collectors.toMap(Order::getId, Order::getOrderCode));
        UUID customerId = earned.getFirst().getCustomerId();
        LocalDateTime now = LocalDateTime.now();
        LoyaltyAccount account = lockAccount(customerId);
        int pointsLeft = pointsFor(refund.getAmount());
        BigDecimal amountLeft = refund.getAmount();
        List<LoyaltyTransaction> reversals = new ArrayList<>();
        for (LoyaltyTransaction earn : earned) {
            if (pointsLeft <= 0 && amountLeft.signum() <= 0) break;
            List<LoyaltyTransaction> rows = byOrder.get(earn.getReferenceId());
            int pointsOpen = rows.stream().mapToInt(LoyaltyTransaction::getPoints).sum();
            BigDecimal amountOpen = rows.stream().map(row -> row.getAmount() == null ? BigDecimal.ZERO : row.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int points = Math.max(0, Math.min(pointsLeft, pointsOpen));
            BigDecimal amount = amountLeft.min(amountOpen).max(BigDecimal.ZERO);
            if (points == 0 && amount.signum() == 0) continue;
            pointsLeft -= points;
            amountLeft = amountLeft.subtract(amount);

            int deducted = Math.min(points, balance(account));
            account.setPointsBalance(balance(account) - deducted);
            account.setTotalSpent(spent(account).subtract(amount).max(BigDecimal.ZERO));
            String note = "Hoàn tiền " + refund.getRefundCode() + " · đơn " + codes.get(earn.getReferenceId())
                    + (deducted < points ? " (không trừ được " + (points - deducted) + " điểm do số dư không đủ)" : "");
            reversals.add(LoyaltyTransaction.builder().customerId(customerId).transactionType(REFUND_REVERSAL)
                    .points(-deducted).amount(amount.negate()).referenceType(ORDER_REFERENCE)
                    .referenceId(earn.getReferenceId()).note(note).createdAt(now).build());
        }
        if (reversals.isEmpty()) return;
        account.setUpdatedAt(now);
        accounts.save(account);
        ledger.saveAll(reversals);
    }

    /** Lên hạng cao nhất mà tổng chi tiêu đạt tới; không bao giờ hạ hạng. */
    private void upgradeTier(UUID customerId, BigDecimal totalSpent, LocalDateTime now) {
        CustomerTier current = tiers.findCurrentByCustomerId(customerId).orElse(null);
        BigDecimal currentMin = current == null || current.getMinTotalSpent() == null
                ? null : current.getMinTotalSpent();
        CustomerTier target = null;
        for (CustomerTier tier : tiers.findAllByOrderByMinTotalSpentAscCodeAsc())
            if (tier.getMinTotalSpent() != null && tier.getMinTotalSpent().compareTo(totalSpent) <= 0) target = tier;
        if (target == null || (current != null && target.getId().equals(current.getId()))) return;
        if (currentMin != null && target.getMinTotalSpent().compareTo(currentMin) <= 0) return;
        assignments.expireCurrent(customerId, now);
        assignments.saveAndFlush(CustomerTierAssignment.builder().customerId(customerId).tierId(target.getId())
                .assignedAt(now).note("Tự nâng hạng khi tổng chi tiêu đạt " + OrderCartPricer.vnd(target.getMinTotalSpent()))
                .build());
    }

    /** Ví điểm của khách (tạo mới nếu chưa có), đã khóa để cộng/trừ đồng thời không lệch số dư. */
    private LoyaltyAccount lockAccount(UUID customerId) {
        jdbc.update("""
                insert into loyalty_accounts (customer_id, points_balance, total_spent, updated_at)
                values (:customerId, 0, 0, now() at time zone 'UTC') on conflict (customer_id) do nothing""",
                new MapSqlParameterSource("customerId", customerId));
        return accounts.findByCustomerIdForUpdate(customerId).orElseThrow();
    }

    private static int balance(LoyaltyAccount account) {
        return account.getPointsBalance() == null ? 0 : account.getPointsBalance();
    }

    private static BigDecimal spent(LoyaltyAccount account) {
        return account.getTotalSpent() == null ? BigDecimal.ZERO : account.getTotalSpent();
    }
}
