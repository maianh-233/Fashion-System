package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.entity.LoyaltyTransaction;
import com.fashionsystem.fashion_system.repository.LoyaltyTransactionRepository;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lịch sử điểm tích lũy của khách: tích theo đơn, trừ khi hoàn tiền, đổi voucher. */
@RestController
@RequestMapping("/api/customers/me/loyalty")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerLoyaltyController {
    private final LoyaltyTransactionRepository ledger;

    /** @param type EARN, REFUND_REVERSAL hoặc REDEEM; points âm khi bị trừ */
    public record Entry(UUID id, String type, int points, BigDecimal amount, String note, LocalDateTime createdAt) {
        static Entry of(LoyaltyTransaction row) {
            return new Entry(row.getId(), row.getTransactionType(), row.getPoints(), row.getAmount(), row.getNote(),
                    row.getCreatedAt());
        }
    }

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public Page<Entry> transactions(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return ledger.findAllByCustomerId(customer.customerId(), request).map(Entry::of);
    }
}
