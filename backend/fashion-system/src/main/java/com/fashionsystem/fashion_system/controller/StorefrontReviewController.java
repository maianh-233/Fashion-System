package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.ReviewDtos;
import com.fashionsystem.fashion_system.service.ProductReviewService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Đánh giá công khai trên trang sản phẩm; khách chưa đăng nhập cũng xem được. */
@RestController
@RequestMapping("/api/storefront/products/{productId}/reviews")
@RequiredArgsConstructor
public class StorefrontReviewController {
    private final ProductReviewService reviews;

    @GetMapping("/summary")
    public ReviewDtos.Summary summary(@PathVariable UUID productId) {
        return reviews.summary(productId);
    }

    @GetMapping
    public Page<ReviewDtos.PublicReview> list(@PathVariable UUID productId,
                                              @RequestParam(required = false) Integer rating,
                                              @RequestParam(defaultValue = "false") boolean withImages,
                                              @PageableDefault(size = 5) Pageable pageable) {
        return reviews.productReviews(productId, rating, withImages, pageable);
    }
}
