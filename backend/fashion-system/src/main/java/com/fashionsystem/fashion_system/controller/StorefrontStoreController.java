package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontStoreDtos;
import com.fashionsystem.fashion_system.service.StorefrontStoreService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hệ thống cửa hàng và cửa hàng còn hàng cho khách: chỉ đọc, không cần đăng nhập. */
@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class StorefrontStoreController {
    private final StorefrontStoreService stores;

    @GetMapping("/stores")
    public List<StorefrontStoreDtos.Store> stores() {
        return stores.stores();
    }

    @GetMapping("/variants/{variantId}/stores")
    public List<StorefrontStoreDtos.Availability> availability(@PathVariable UUID variantId) {
        return stores.availability(variantId);
    }
}
