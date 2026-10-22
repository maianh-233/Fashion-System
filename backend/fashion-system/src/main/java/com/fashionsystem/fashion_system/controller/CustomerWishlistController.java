package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.WishlistDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.CustomerWishlistService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sản phẩm yêu thích của khách đã đăng nhập. */
@RestController
@RequestMapping("/api/customers/me/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerWishlistController {
    private final CustomerWishlistService wishlist;

    @GetMapping
    public WishlistDtos.Wishlist view(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return wishlist.view(customer.customerId());
    }

    @PutMapping("/{productId}")
    public WishlistDtos.Wishlist add(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID productId) {
        return wishlist.add(customer.customerId(), productId);
    }

    @DeleteMapping("/{productId}")
    public WishlistDtos.Wishlist remove(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                        @PathVariable UUID productId) {
        return wishlist.remove(customer.customerId(), productId);
    }

    /** Gộp danh sách đã lưu trên trình duyệt (khi chưa đăng nhập) vào tài khoản. */
    @PostMapping("/merge")
    public WishlistDtos.Wishlist merge(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                       @Valid @RequestBody WishlistDtos.GuestWishlist request) {
        return wishlist.merge(customer.customerId(), request);
    }
}
