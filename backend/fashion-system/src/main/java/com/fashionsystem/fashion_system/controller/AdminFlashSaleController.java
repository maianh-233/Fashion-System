package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.FlashSaleDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.FlashSaleService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Quản lý flash sale (toàn hệ thống, quyền khuyến mãi). */
@RestController
@RequestMapping("/api/admin/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminFlashSaleController {
    private final FlashSaleService flashSales;

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public Page<FlashSaleDtos.AdminSale> list(@PageableDefault(size = 20) Pageable pageable) {
        return flashSales.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public FlashSaleDtos.AdminSale get(@PathVariable UUID id) {
        return flashSales.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public FlashSaleDtos.AdminSale create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody FlashSaleDtos.SaleRequest request) {
        return flashSales.create(user.userId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public FlashSaleDtos.AdminSale update(@PathVariable UUID id, @Valid @RequestBody FlashSaleDtos.SaleRequest request) {
        return flashSales.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public FlashSaleDtos.AdminSale setActive(@PathVariable UUID id, @RequestParam boolean value) {
        return flashSales.setActive(id, value);
    }
}
