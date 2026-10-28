package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.StockAlertDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.StockAlertService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Báo khi có hàng" của khách đã đăng nhập, theo biến thể (màu + size). */
@RestController
@RequestMapping("/api/customers/me/stock-alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerStockAlertController {
    private final StockAlertService alerts;

    @GetMapping
    public List<StockAlertDtos.Alert> mine(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return alerts.mine(customer.customerId());
    }

    /** Biến thể của sản phẩm mà khách đang chờ. */
    @GetMapping("/pending")
    public StockAlertDtos.Pending pending(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                          @RequestParam UUID productId) {
        return alerts.pending(customer.customerId(), productId);
    }

    @PutMapping("/{variantId}")
    public StockAlertDtos.Pending subscribe(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID variantId) {
        return alerts.subscribe(customer.customerId(), variantId);
    }

    @DeleteMapping("/{variantId}")
    public StockAlertDtos.Pending unsubscribe(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                             @PathVariable UUID variantId) {
        return alerts.unsubscribe(customer.customerId(), variantId);
    }
}
