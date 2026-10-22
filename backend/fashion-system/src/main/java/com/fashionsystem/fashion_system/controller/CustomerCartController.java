package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.CartDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.CustomerCartService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Giỏ hàng của khách đã đăng nhập. */
@RestController
@RequestMapping("/api/customers/me/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerCartController {
    private final CustomerCartService cart;

    @GetMapping
    public CartDtos.Cart view(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return cart.view(customer.customerId());
    }

    @PostMapping("/items")
    public CartDtos.Cart add(@AuthenticationPrincipal AuthenticatedCustomer customer,
                             @Valid @RequestBody CartDtos.AddItem request) {
        return cart.add(customer.customerId(), request);
    }

    @PatchMapping("/items/{variantId}")
    public CartDtos.Cart update(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID variantId,
                                @Valid @RequestBody CartDtos.UpdateItem request) {
        return cart.update(customer.customerId(), variantId, request);
    }

    @DeleteMapping("/items/{variantId}")
    public CartDtos.Cart remove(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID variantId) {
        return cart.remove(customer.customerId(), variantId);
    }

    @PutMapping("/selection")
    public CartDtos.Cart selectAll(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                   @Valid @RequestBody CartDtos.SelectAll request) {
        return cart.selectAll(customer.customerId(), request.selected());
    }

    /** Gộp giỏ đã thêm trên trình duyệt (khi chưa đăng nhập) vào giỏ tài khoản. */
    @PostMapping("/merge")
    public CartDtos.Cart merge(@AuthenticationPrincipal AuthenticatedCustomer customer,
                               @Valid @RequestBody CartDtos.GuestCart request) {
        return cart.merge(customer.customerId(), request);
    }
}
