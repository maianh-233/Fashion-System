package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import com.fashionsystem.fashion_system.service.ProductRecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gợi ý "thường được mua cùng" và "có thể bạn cũng thích" trên trang sản phẩm; công khai. */
@RestController
@RequestMapping("/api/storefront/products/{productId}/recommendations")
@RequiredArgsConstructor
public class StorefrontRecommendationController {
    private final ProductRecommendationService recommendations;

    @GetMapping
    public StorefrontDtos.Recommendations recommend(@PathVariable UUID productId,
                                                    @RequestParam(defaultValue = "" + ProductRecommendationService.DEFAULT_LIMIT) int limit) {
        return recommendations.recommend(productId, limit);
    }
}
