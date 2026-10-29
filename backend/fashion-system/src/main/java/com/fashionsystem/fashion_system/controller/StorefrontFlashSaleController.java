package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.FlashSaleDtos;
import com.fashionsystem.fashion_system.service.FlashSaleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Flash sale đang chạy cho trang chủ / trang sản phẩm; công khai. */
@RestController
@RequestMapping("/api/storefront/flash-sales")
@RequiredArgsConstructor
public class StorefrontFlashSaleController {
    private final FlashSaleService flashSales;

    @GetMapping("/running")
    public List<FlashSaleDtos.LiveSale> running() {
        return flashSales.running();
    }
}
