package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos;
import com.fashionsystem.fashion_system.service.SizeAdvisorService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bảng size và gợi ý theo số đo cho khách chưa đăng nhập (không có lịch sử mua). */
@RestController
@RequiredArgsConstructor
public class StorefrontSizeGuideController {
    private final SizeAdvisorService sizes;

    @GetMapping("/api/storefront/products/{productId}/size-guide")
    public SizeAdvisorDtos.Guide guide(@PathVariable UUID productId,
                                       @RequestParam(required = false) Integer heightCm,
                                       @RequestParam(required = false) BigDecimal weightKg) {
        return sizes.guide(productId, null, heightCm, weightKg);
    }
}
