package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.promotion.CustomerPromotionResponse;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.PromotionRedemptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Khách hàng xem khuyến mãi đổi điểm, đổi điểm lấy khuyến mãi và xem các khuyến mãi đã đổi. */
@RestController
@RequestMapping("/api/customers/me/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPromotionController {
    private final PromotionRedemptionService redemptionService;

    @GetMapping("/redeemable")
    public Page<PromotionDto> redeemable(
            @AuthenticationPrincipal AuthenticatedCustomer customer,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "endDate") Pageable pageable) {
        return redemptionService.redeemable(customer.customerId(), keyword, pageable);
    }

    @GetMapping
    public Page<CustomerPromotionResponse> mine(
            @AuthenticationPrincipal AuthenticatedCustomer customer,
            @PageableDefault(size = 20, sort = "redeemedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return redemptionService.mine(customer.customerId(), pageable);
    }

    @PostMapping("/{promotionId}/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerPromotionResponse redeem(
            @AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID promotionId) {
        return redemptionService.redeem(customer.customerId(), promotionId);
    }
}
